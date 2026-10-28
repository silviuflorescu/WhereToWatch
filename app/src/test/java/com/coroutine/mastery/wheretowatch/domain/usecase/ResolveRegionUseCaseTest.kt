package com.coroutine.mastery.wheretowatch.domain.usecase

import com.coroutine.mastery.wheretowatch.domain.model.RegionSelection
import com.coroutine.mastery.wheretowatch.domain.model.RegionSource
import com.coroutine.mastery.wheretowatch.domain.model.ResolvedRegion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ResolveRegionUseCaseTest {

    private var detections = 0
    private val useCase = ResolveRegionUseCase {
        detections++
        ResolvedRegion("RO", RegionSource.Location)
    }

    @Test
    fun `manual selection uses that country without detecting`() = runTest {
        assertEquals(ResolvedRegion("DE", RegionSource.Manual), useCase(RegionSelection.Manual("DE")))
        assertEquals(0, detections)
    }

    @Test
    fun `auto selection uses the detected country`() = runTest {
        assertEquals(ResolvedRegion("RO", RegionSource.Location), useCase(RegionSelection.Auto))
        assertEquals(1, detections)
    }
}
