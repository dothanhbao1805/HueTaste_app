package com.example.huetaste.data.local

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import com.example.huetaste.domain.model.GeoPoint
import com.example.huetaste.domain.repository.LocationProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import kotlin.coroutines.resume

/**
 * Approximate location from the platform [LocationManager] (no Play services). A recent cached fix is used when there
 * is one; otherwise one fresh fix is requested from the network provider, waiting at most [FIX_TIMEOUT_MS].
 */
class DeviceLocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : LocationProvider {

    private val locationManager: LocationManager?
        get() = ContextCompat.getSystemService(context, LocationManager::class.java)

    @SuppressLint("MissingPermission") // Checked by hasPermission().
    override suspend fun current(): GeoPoint? {
        if (!hasPermission()) return null
        val manager = locationManager ?: return null
        val cached = CACHED_PROVIDERS
            .mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
            .filter { System.currentTimeMillis() - it.time <= MAX_CACHED_AGE_MS }
            .maxByOrNull { it.time }
        val location = cached ?: withTimeoutOrNull(FIX_TIMEOUT_MS) { freshFix(manager) }
        return location?.let { GeoPoint(it.latitude, it.longitude) }
    }

    private fun hasPermission(): Boolean = listOf(
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    @SuppressLint("MissingPermission")
    private suspend fun freshFix(manager: LocationManager): Location? {
        val provider = LocationManager.NETWORK_PROVIDER
        if (!runCatching { manager.isProviderEnabled(provider) }.getOrDefault(false)) return null
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
        val CACHED_PROVIDERS = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
        const val MAX_CACHED_AGE_MS = 10 * 60 * 1000L
        const val FIX_TIMEOUT_MS = 5_000L
    }
}
