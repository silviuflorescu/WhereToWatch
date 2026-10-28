package com.coroutine.mastery.wheretowatch.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class AndroidLocationSource @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationSource {

    private val locationManager: LocationManager? = context.getSystemService(LocationManager::class.java)

    override suspend fun coordinates(): Coordinates? {
        val locationManager = locationManager ?: return null
        if (!hasLocationPermission()) return null
        val location = locationManager.lastKnownLocation() ?: locationManager.currentLocation()
        return location?.let { Coordinates(it.latitude, it.longitude) }
    }

    private fun hasLocationPermission(): Boolean =
        listOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION).any {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }

    @SuppressLint("MissingPermission")
    private fun LocationManager.lastKnownLocation(): Location? =
        getProviders(true)
            .mapNotNull { provider -> runCatching { getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull { it.time }

    @SuppressLint("MissingPermission")
    private suspend fun LocationManager.currentLocation(): Location? {
        val provider = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && isProviderEnabled(LocationManager.FUSED_PROVIDER) ->
                LocationManager.FUSED_PROVIDER
            isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> return null
        }
        return suspendCancellableCoroutine { continuation ->
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }
            LocationManagerCompat.getCurrentLocation(
                this,
                provider,
                cancellationSignal,
                ContextCompat.getMainExecutor(context),
            ) { location -> continuation.resume(location) }
        }
    }
}
