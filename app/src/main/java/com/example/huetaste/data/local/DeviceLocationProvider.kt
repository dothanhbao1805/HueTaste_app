package com.example.huetaste.data.local

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.example.huetaste.di.IoDispatcher
import com.example.huetaste.domain.model.DeviceArea
import com.example.huetaste.domain.model.GeoPoint
import com.example.huetaste.domain.repository.LocationProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Location from the platform [LocationManager] (no Play services). [current] prefers a recent cached fix for a quick
 * answer; [currentArea] asks for a new fix first, so each session of the home screen reflects where the user is now.
 */
class DeviceLocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: SharedPreferences,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : LocationProvider {

    private val locationManager: LocationManager?
        get() = ContextCompat.getSystemService(context, LocationManager::class.java)

    @SuppressLint("MissingPermission") // Checked by hasPermission().
    override suspend fun current(): GeoPoint? {
        if (!hasPermission()) return null
        val manager = locationManager ?: return null
        val location = recentCached(manager) ?: withTimeoutOrNull(FIX_TIMEOUT_MS) { freshFix(manager) }
        return location?.toGeoPoint()
    }

    override suspend fun currentArea(): DeviceArea {
        if (!hasPermission()) return DeviceArea.PermissionMissing
        val isPrecise = hasFinePermission()
        val manager = locationManager ?: return DeviceArea.Found(null, null, isPrecise)
        if (!LocationManagerCompat.isLocationEnabled(manager)) return DeviceArea.LocationOff
        // Never an older fix than recentCached: it could place a user who has left Huế back in Huế.
        val location = withTimeoutOrNull(AREA_FIX_TIMEOUT_MS) { freshFix(manager) }
            ?: recentCached(manager)
            ?: return DeviceArea.Found(null, null, isPrecise)
        return DeviceArea.Found(areaName(location), location.toGeoPoint(), isPrecise)
    }

    override suspend fun claimPermissionPrompt(): Boolean = withContext(ioDispatcher) {
        val first = !preferences.getBoolean(KEY_PROMPT_CLAIMED, false)
        if (first) preferences.edit().putBoolean(KEY_PROMPT_CLAIMED, true).apply()
        first
    }

    @SuppressLint("MissingPermission") // Every caller has checked hasPermission().
    private fun recentCached(manager: LocationManager): Location? = CACHED_PROVIDERS
        .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
        .filter { System.currentTimeMillis() - it.time <= MAX_CACHED_AGE_MS }
        .maxByOrNull { it.time }

    private fun Location.toGeoPoint() = GeoPoint(latitude, longitude)

    /** The narrowest and widest names of the address, such as "Hải Châu, Đà Nẵng", never a street address. */
    private suspend fun areaName(location: Location): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale("vi", "VN"))
        val address = withTimeoutOrNull(GEOCODE_TIMEOUT_MS) { firstAddress(geocoder, location) } ?: return null
        val names = listOf(address.subLocality, address.locality, address.subAdminArea, address.adminArea)
            .mapNotNull { part -> part?.trim()?.replaceFirst(CITY_PREFIX, "")?.takeIf { it.isNotEmpty() } }
            .distinct()
        return listOfNotNull(names.firstOrNull(), names.lastOrNull()).distinct().joinToString(", ").ifEmpty { null }
    }

    private suspend fun firstAddress(geocoder: Geocoder, location: Location): Address? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                geocoder.getFromLocation(
                    location.latitude,
                    location.longitude,
                    1,
                    object : Geocoder.GeocodeListener {
                        override fun onGeocode(addresses: MutableList<Address>) {
                            if (continuation.isActive) continuation.resume(addresses.firstOrNull())
                        }

                        override fun onError(errorMessage: String?) {
                            if (continuation.isActive) continuation.resume(null)
                        }
                    },
                )
            }
        } else {
            withContext(ioDispatcher) {
                runCatching {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()
                }.getOrNull()
            }
        }

    private fun hasPermission(): Boolean = listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    private fun hasFinePermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /** The GPS provider refuses an approximate-only permission, so it is only a candidate with the fine one. */
    private fun freshFixProvider(manager: LocationManager): String? = listOfNotNull(
        LocationManager.FUSED_PROVIDER.takeIf { Build.VERSION.SDK_INT >= Build.VERSION_CODES.S },
        LocationManager.NETWORK_PROVIDER,
        LocationManager.GPS_PROVIDER.takeIf { hasFinePermission() },
    ).firstOrNull { provider -> runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false) }

    @SuppressLint("MissingPermission")
    private suspend fun freshFix(manager: LocationManager): Location? {
        val provider = freshFixProvider(manager) ?: return null
        return suspendCancellableCoroutine { continuation ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val signal = CancellationSignal()
                continuation.invokeOnCancellation { signal.cancel() }
                manager.getCurrentLocation(provider, signal, ContextCompat.getMainExecutor(context)) { location ->
                    if (continuation.isActive) continuation.resume(location)
                }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(location)
                    }

                    @Deprecated("Required on API 28 and lower")
                    override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) = Unit

                    override fun onProviderDisabled(provider: String) {
                        manager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
                continuation.invokeOnCancellation { manager.removeUpdates(listener) }
                manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
            }
        }
    }

    private companion object {
        val CACHED_PROVIDERS = listOf(
            LocationManager.FUSED_PROVIDER,
            LocationManager.NETWORK_PROVIDER,
            LocationManager.GPS_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
        )
        const val MAX_CACHED_AGE_MS = 10 * 60 * 1000L
        const val FIX_TIMEOUT_MS = 5_000L
        const val AREA_FIX_TIMEOUT_MS = 8_000L
        const val GEOCODE_TIMEOUT_MS = 5_000L
        val CITY_PREFIX = Regex("^(Thành phố|Tỉnh|TP\\.?)\\s+", RegexOption.IGNORE_CASE)
        const val KEY_PROMPT_CLAIMED = "location_permission_prompt_claimed"
    }
}
