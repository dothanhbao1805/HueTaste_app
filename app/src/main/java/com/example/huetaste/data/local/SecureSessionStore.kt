package com.example.huetaste.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.huetaste.domain.model.AuthSession
import com.example.huetaste.domain.model.AuthState
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remembers who is signed in across launches. The session is encrypted with AES-GCM under a key that never
 * leaves the Android Keystore and is kept in its own preferences file, which backup rules exclude.
 *
 * Reads and writes touch disk and the Keystore, so call them off the main thread. The session is cached in
 * memory after the first read, which keeps the per-request token lookup of the network layer cheap.
 */
@Singleton
class SecureSessionStore @Inject constructor(
    @ApplicationContext context: Context,
    private val gson: Gson,
) {
    private val preferences = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
    private val lock = Any()

    @Volatile private var cachedSession: AuthSession? = null
    @Volatile private var loaded = false

    private val _changes = MutableSharedFlow<AuthState>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Emits the new state after every change to the session or the guest flag. */
    val changes: SharedFlow<AuthState> = _changes.asSharedFlow()

    fun session(): AuthSession? {
        if (!loaded) {
            synchronized(lock) {
                if (!loaded) {
                    cachedSession = readSession()
                    loaded = true
                }
            }
        }
        return cachedSession
    }

    fun isGuest(): Boolean = preferences.getBoolean(KEY_GUEST, false)

    fun state(): AuthState = when {
        session() != null -> AuthState.SIGNED_IN
        isGuest() -> AuthState.GUEST
        else -> AuthState.SIGNED_OUT
    }

    fun save(session: AuthSession) {
        synchronized(lock) {
            cachedSession = session
            loaded = true
            val encrypted = try {
                encrypt(gson.toJson(session))
            } catch (e: Exception) {
                // Without a usable Keystore the session lives only in memory; the next launch asks for a login.
                null
            }
            preferences.edit().apply {
                if (encrypted != null) putString(KEY_SESSION, encrypted) else remove(KEY_SESSION)
                putBoolean(KEY_GUEST, false)
            }.commit()
        }
        _changes.tryEmit(AuthState.SIGNED_IN)
    }

    fun markGuest() {
        synchronized(lock) {
            cachedSession = null
            loaded = true
            preferences.edit().remove(KEY_SESSION).putBoolean(KEY_GUEST, true).commit()
        }
        _changes.tryEmit(AuthState.GUEST)
    }

    fun clear() {
        synchronized(lock) {
            cachedSession = null
            loaded = true
            preferences.edit().remove(KEY_SESSION).putBoolean(KEY_GUEST, false).commit()
        }
        _changes.tryEmit(AuthState.SIGNED_OUT)
    }

    private fun readSession(): AuthSession? {
        val stored = preferences.getString(KEY_SESSION, null) ?: return null
        return try {
            gson.fromJson(decrypt(stored), AuthSession::class.java)
        } catch (e: Exception) {
            // Keystore and cipher failures, bad Base64 and Gson's damaged-payload errors all mean the same thing.
            discardUnreadableSession()
        }
    }

    /** The key was lost (new lock screen, restored device) or the data is damaged: sign the user out, never crash. */
    private fun discardUnreadableSession(): AuthSession? {
        preferences.edit().remove(KEY_SESSION).commit()
        deleteKey()
        return null
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
        val bytes = Base64.decode(stored, Base64.NO_WRAP)
        require(bytes.size > IV_SIZE) { "Stored session is too short" }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, bytes, 0, IV_SIZE))
        return String(cipher.doFinal(bytes, IV_SIZE, bytes.size - IV_SIZE), Charsets.UTF_8)
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_BITS)
                .build(),
        )
        return generator.generateKey()
    }

    private fun deleteKey() {
        try {
            KeyStore.getInstance(ANDROID_KEY_STORE).apply { load(null) }.deleteEntry(KEY_ALIAS)
        } catch (e: Exception) {
            // Nothing else to clean up; a new key is generated on the next save.
        }
    }

    private companion object {
        const val FILE_NAME = "auth_session"
        const val KEY_SESSION = "session"
        const val KEY_GUEST = "guest"
        const val ANDROID_KEY_STORE = "AndroidKeyStore"
        const val KEY_ALIAS = "huetaste_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BITS = 256
        const val IV_SIZE = 12
        const val TAG_BITS = 128
    }
}
