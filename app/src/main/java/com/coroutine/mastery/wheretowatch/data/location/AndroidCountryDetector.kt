package com.coroutine.mastery.wheretowatch.data.location

import com.coroutine.mastery.wheretowatch.core.util.suspendRunCatching
import com.coroutine.mastery.wheretowatch.domain.model.RegionSource
import com.coroutine.mastery.wheretowatch.domain.model.ResolvedRegion
import com.coroutine.mastery.wheretowatch.domain.repository.CountryDetector
import javax.inject.Inject
import kotlinx.coroutines.withTimeoutOrNull

class AndroidCountryDetector @Inject constructor(
    private val locationSource: LocationSource,
    private val geocoderSource: GeocoderSource,
    private val networkCountrySource: NetworkCountrySource,
    private val deviceLocaleSource: DeviceLocaleSource,
) : CountryDetector {

    override suspend fun detect(): ResolvedRegion {
        countryFromLocation()?.let { return ResolvedRegion(it, RegionSource.Location) }
        networkCountrySource.countryCode().toCountryCode()?.let { return ResolvedRegion(it, RegionSource.Network) }
        deviceLocaleSource.countryCode().toCountryCode()?.let { return ResolvedRegion(it, RegionSource.DeviceLocale) }
        return ResolvedRegion(DEFAULT_COUNTRY, RegionSource.Default)
    }

    private suspend fun countryFromLocation(): String? =
        withTimeoutOrNull(LOCATION_TIMEOUT_MILLIS) {
            suspendRunCatching {
                locationSource.coordinates()?.let { geocoderSource.countryCode(it) }
            }.getOrNull()
        }.toCountryCode()

    private fun String?.toCountryCode(): String? =
        this?.trim()?.uppercase()?.takeIf { it.length == 2 && it.all(Char::isLetter) }

    companion object {
        const val DEFAULT_COUNTRY = "US"
        const val LOCATION_TIMEOUT_MILLIS = 10_000L
    }
}
