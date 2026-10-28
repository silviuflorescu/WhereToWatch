package com.coroutine.mastery.wheretowatch.ui.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coroutine.mastery.wheretowatch.domain.usecase.GetCastUseCase
import com.coroutine.mastery.wheretowatch.domain.usecase.GetMovieDetailsUseCase
import com.coroutine.mastery.wheretowatch.domain.usecase.GetRecommendationsUseCase
import com.coroutine.mastery.wheretowatch.domain.usecase.GetTrailerUseCase
import com.coroutine.mastery.wheretowatch.domain.usecase.GetWatchProvidersUseCase
import com.coroutine.mastery.wheretowatch.ui.search.ProvidersState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MovieDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getMovieDetails: GetMovieDetailsUseCase,
    private val getCast: GetCastUseCase,
    private val getTrailer: GetTrailerUseCase,
    private val getRecommendations: GetRecommendationsUseCase,
    private val getWatchProviders: GetWatchProvidersUseCase,
) : ViewModel() {

    private val movieId: Int = checkNotNull(savedStateHandle["movieId"]) { "movieId is missing" }

    private val retryTrigger = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    ).apply { tryEmit(Unit) }

    val uiState: StateFlow<MovieDetailsUiState> =
        retryTrigger.flatMapLatest { loadSections() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MovieDetailsUiState())

    fun onRetry() {
        retryTrigger.tryEmit(Unit)
    }

    private fun loadSections(): Flow<MovieDetailsUiState> = combine(
        section { getMovieDetails(movieId) },
        section { getCast(movieId) },
        section { getTrailer(movieId) },
        section { getRecommendations(movieId) },
        providersSection(),
    ) { details, cast, trailer, recommendations, providers ->
        MovieDetailsUiState(details, cast, trailer, recommendations, providers)
    }

    private fun <T> section(load: suspend () -> Result<T>): Flow<Section<T>> = flow {
        emit(Section.Loading)
        emit(load().fold(onSuccess = { Section.Loaded(it) }, onFailure = { Section.Failed(it) }))
    }

    private fun providersSection(): Flow<ProvidersState> = flow {
        emit(ProvidersState.Loading)
        emit(
            getWatchProviders(movieId).fold(
                onSuccess = { ProvidersState.Loaded(it) },
                onFailure = { ProvidersState.Failed },
            )
        )
    }
}
