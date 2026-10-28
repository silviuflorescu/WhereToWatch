package com.coroutine.mastery.wheretowatch.ui.region

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coroutine.mastery.wheretowatch.domain.model.Country
import com.coroutine.mastery.wheretowatch.domain.model.RegionSelection
import com.coroutine.mastery.wheretowatch.domain.model.ResolvedRegion
import com.coroutine.mastery.wheretowatch.domain.repository.CountryRepository
import com.coroutine.mastery.wheretowatch.domain.repository.RegionPreferences
import com.coroutine.mastery.wheretowatch.domain.usecase.ResolveRegionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RegionViewModel @Inject constructor(
    private val resolveRegion: ResolveRegionUseCase,
    private val regionPreferences: RegionPreferences,
    private val countryRepository: CountryRepository,
) : ViewModel() {

    private val detectTrigger = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    ).apply { tryEmit(Unit) }

    private val countriesReload = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    ).apply { tryEmit(Unit) }

    private val regionStatus: StateFlow<RegionStatus?> =
        combine(regionPreferences.selection, detectTrigger) { selection, _ -> selection }
            .transformLatest { selection ->
                emit(RegionStatus(selection, resolved = null))
                emit(RegionStatus(selection, resolveRegion(selection)))
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, null)

    private val countries: Flow<List<Country>> = countriesReload
        .mapLatest { countryRepository.getCountries().getOrDefault(emptyList()) }
        .onStart { emit(emptyList()) }

    val regionUiState: StateFlow<RegionUiState> =
        combine(regionStatus.filterNotNull(), countries) { status, countries ->
            RegionUiState(status.selection, status.resolved, countries)
        }.stateIn(viewModelScope, SharingStarted.Lazily, RegionUiState())

    val shouldAskLocationPermission: StateFlow<Boolean> =
        combine(regionPreferences.selection, regionPreferences.locationPermissionAsked) { selection, asked ->
            selection == RegionSelection.Auto && !asked
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun onRegionSelected(selection: RegionSelection) {
        viewModelScope.launch { regionPreferences.setSelection(selection) }
    }

    fun onLocationPermissionRequested() {
        viewModelScope.launch { regionPreferences.setLocationPermissionAsked() }
    }

    fun onLocationPermissionResult() {
        detectTrigger.tryEmit(Unit)
    }

    fun onCountryMenuOpened() {
        if (regionUiState.value.countries.isEmpty()) countriesReload.tryEmit(Unit)
    }

    private data class RegionStatus(val selection: RegionSelection, val resolved: ResolvedRegion?)
}
