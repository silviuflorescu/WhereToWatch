package com.coroutine.mastery.wheretowatch.ui.region

import com.coroutine.mastery.wheretowatch.domain.model.Country
import com.coroutine.mastery.wheretowatch.domain.model.RegionSelection
import com.coroutine.mastery.wheretowatch.domain.model.RegionSource
import com.coroutine.mastery.wheretowatch.domain.model.ResolvedRegion
import com.coroutine.mastery.wheretowatch.domain.repository.CountryDetector
import com.coroutine.mastery.wheretowatch.domain.repository.CountryRepository
import com.coroutine.mastery.wheretowatch.domain.repository.RegionPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow

class FakeRegionPreferences(initial: RegionSelection = RegionSelection.Auto) : RegionPreferences {
    override val selection = MutableStateFlow(initial)
    override val locationPermissionAsked = MutableStateFlow(false)

    override suspend fun setSelection(selection: RegionSelection) {
        this.selection.value = selection
    }

    override suspend fun setLocationPermissionAsked() {
        locationPermissionAsked.value = true
    }
}

class FakeCountryDetector : CountryDetector {
    var delayMillis = 0L
    var result = ResolvedRegion("US", RegionSource.Location)

    var detections = 0
        private set
    var cancellations = 0
        private set

    override suspend fun detect(): ResolvedRegion {
        detections++
        try {
            delay(delayMillis)
        } catch (e: CancellationException) {
            cancellations++
            throw e
        }
        return result
    }
}

class FakeCountryRepository : CountryRepository {
    var countries = listOf(Country("DE", "Germany"), Country("RO", "Romania"), Country("US", "United States"))

    override suspend fun getCountries(): Result<List<Country>> = Result.success(countries)
}
