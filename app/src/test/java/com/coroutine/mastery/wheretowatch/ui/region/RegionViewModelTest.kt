package com.coroutine.mastery.wheretowatch.ui.region

import com.coroutine.mastery.wheretowatch.MainDispatcherRule
import com.coroutine.mastery.wheretowatch.domain.model.RegionSelection
import com.coroutine.mastery.wheretowatch.domain.model.RegionSource
import com.coroutine.mastery.wheretowatch.domain.model.ResolvedRegion
import com.coroutine.mastery.wheretowatch.domain.usecase.ResolveRegionUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val regionPreferences = FakeRegionPreferences()
    private val countryDetector = FakeCountryDetector()

    private val viewModel by lazy {
        RegionViewModel(
            resolveRegion = ResolveRegionUseCase(countryDetector),
            regionPreferences = regionPreferences,
            countryRepository = FakeCountryRepository(),
        )
    }

    private fun TestScope.collectRegion(): Job =
        backgroundScope.launch { viewModel.regionUiState.collect {} }.also { runCurrent() }

    @Test
    fun `Auto resolves to the detected country`() = runTest {
        countryDetector.result = ResolvedRegion("RO", RegionSource.Location)
        collectRegion()

        advanceUntilIdle()

        assertEquals(ResolvedRegion("RO", RegionSource.Location), viewModel.regionUiState.value.resolved)
    }

    @Test
    fun `choosing a country saves it and uses it without detecting`() = runTest {
        collectRegion()
        advanceUntilIdle()

        viewModel.onRegionSelected(RegionSelection.Manual("DE"))
        advanceUntilIdle()

        assertEquals(RegionSelection.Manual("DE"), regionPreferences.selection.value)
        assertEquals(ResolvedRegion("DE", RegionSource.Manual), viewModel.regionUiState.value.resolved)
        assertEquals(1, countryDetector.detections)
    }

    @Test
    fun `choosing a country while Auto is detecting cancels the detection`() = runTest {
        countryDetector.delayMillis = 5_000
        collectRegion()
        advanceTimeBy(100)
        assertTrue(viewModel.regionUiState.value.isDetecting)

        viewModel.onRegionSelected(RegionSelection.Manual("DE"))
        advanceUntilIdle()

        assertEquals(1, countryDetector.cancellations)
        assertEquals(ResolvedRegion("DE", RegionSource.Manual), viewModel.regionUiState.value.resolved)
    }

    @Test
    fun `a permission result detects the country again`() = runTest {
        collectRegion()
        advanceUntilIdle()

        viewModel.onLocationPermissionResult()
        advanceUntilIdle()

        assertEquals(2, countryDetector.detections)
    }

    @Test
    fun `asks for the location permission only once`() = runTest {
        backgroundScope.launch { viewModel.shouldAskLocationPermission.collect {} }
        runCurrent()
        assertTrue(viewModel.shouldAskLocationPermission.value)

        viewModel.onLocationPermissionRequested()
        runCurrent()

        assertEquals(false, viewModel.shouldAskLocationPermission.value)
    }

    @Test
    fun `the country list is loaded for the dropdown`() = runTest {
        collectRegion()

        assertEquals(listOf("DE", "RO", "US"), viewModel.regionUiState.value.countries.map { it.code })
    }

    @Test
    fun `switching screens doesn't detect the country again`() = runTest {
        val collector = collectRegion()
        advanceUntilIdle()

        collector.cancel()
        advanceTimeBy(60_000)
        collectRegion()
        advanceUntilIdle()

        assertEquals(1, countryDetector.detections)
        assertEquals(ResolvedRegion("US", RegionSource.Location), viewModel.regionUiState.value.resolved)
    }
}
