package com.coroutine.mastery.wheretowatch.domain.usecase

import com.coroutine.mastery.wheretowatch.domain.model.RegionSelection
import com.coroutine.mastery.wheretowatch.domain.model.RegionSource
import com.coroutine.mastery.wheretowatch.domain.model.ResolvedRegion
import com.coroutine.mastery.wheretowatch.domain.repository.CountryDetector
import javax.inject.Inject

class ResolveRegionUseCase @Inject constructor(
    private val countryDetector: CountryDetector,
) {
    suspend operator fun invoke(selection: RegionSelection): ResolvedRegion = when (selection) {
        is RegionSelection.Manual -> ResolvedRegion(selection.code, RegionSource.Manual)
        RegionSelection.Auto -> countryDetector.detect()
    }
}
