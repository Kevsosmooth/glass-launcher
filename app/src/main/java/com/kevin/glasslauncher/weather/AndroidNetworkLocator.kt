package com.kevin.glasslauncher.weather

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

private const val FIX_TIMEOUT_MS = 10_000L
private const val MAX_FIX_AGE_MS = 6 * 60 * 60_000L

/**
 * Android's network (Wi-Fi) location. Only used when coarse location is already granted (Glass
 * asks for it on its Wi-Fi page; this never prompts) and the network provider is on. A recent
 * last-known fix is used first; otherwise one fresh fix, given up after 10 s.
 */
class AndroidNetworkLocator(
    context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) : NetworkLocator {
    private val app = context.applicationContext
    private val manager: LocationManager? = app.getSystemService(LocationManager::class.java)

    override suspend fun locate(): Coordinates? {
        val lm = manager ?: return null
        val granted = app.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!granted || !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) return null
        val fix = lastKnown(lm) ?: currentFix(lm) ?: return null
        return Coordinates(fix.latitude, fix.longitude)
    }

    /** The newest network or passive fix under [MAX_FIX_AGE_MS] old. */
    private fun lastKnown(lm: LocationManager): Location? {
        val now = clock()
        return listOf(LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .mapNotNull { provider ->
                try {
                    lm.getLastKnownLocation(provider)
                } catch (e: SecurityException) {
                    // The permission was revoked between the check and the call.
                    null
                }
            }
            .filter { now - it.time in 0 until MAX_FIX_AGE_MS }
            .maxByOrNull { it.time }
    }

    private suspend fun currentFix(lm: LocationManager): Location? = withTimeoutOrNull(FIX_TIMEOUT_MS) {
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            try {
                LocationManagerCompat.getCurrentLocation(lm, LocationManager.NETWORK_PROVIDER, signal, Runnable::run) { fix ->
                    if (cont.isActive) cont.resume(fix)
                }
            } catch (e: SecurityException) {
                cont.resume(null)
            }
        }
    }
}
