package com.coroutine.mastery.wheretowatch.data.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import com.coroutine.mastery.wheretowatch.core.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

class AndroidGeocoderSource @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : GeocoderSource {

    override suspend fun countryCode(coordinates: Coordinates): String? {
        if (!Geocoder.isPresent()) return null
        val geocoder = Geocoder(context, Locale.ENGLISH)
        val address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.addressAsync(coordinates)
        } else {
            geocoder.addressBlocking(coordinates)
        }
        return address?.countryCode
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun Geocoder.addressAsync(coordinates: Coordinates): Address? =
        suspendCancellableCoroutine { continuation ->
            getFromLocation(
                coordinates.latitude,
                coordinates.longitude,
                1,
                object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        continuation.resume(addresses.firstOrNull())
                    }

                    override fun onError(errorMessage: String?) {
                        continuation.resume(null)
                    }
                },
            )
        }

    private suspend fun Geocoder.addressBlocking(coordinates: Coordinates): Address? =
        withContext(ioDispatcher) {
            @Suppress("DEPRECATION")
            getFromLocation(coordinates.latitude, coordinates.longitude, 1)?.firstOrNull()
        }
}
