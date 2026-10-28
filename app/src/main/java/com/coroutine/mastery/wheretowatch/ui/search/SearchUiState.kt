package com.coroutine.mastery.wheretowatch.ui.search

import com.coroutine.mastery.wheretowatch.domain.model.Movie
import com.coroutine.mastery.wheretowatch.domain.model.WatchProviders

sealed interface SearchUiState {
    data object Idle : SearchUiState

    data object Loading : SearchUiState

    data class Empty(val query: String) : SearchUiState

    data class Error(val error: Throwable) : SearchUiState

    data class Results(
        val movies: List<Movie>,
        val providers: Map<Int, ProvidersState>,
        val nextPage: NextPageState = NextPageState.End,
    ) : SearchUiState
}

sealed interface NextPageState {
    data object Idle : NextPageState
    data object Loading : NextPageState
    data class Failed(val error: Throwable) : NextPageState
    data object End : NextPageState
}

sealed interface ProvidersState {
    data object Loading : ProvidersState

    data class Loaded(val byCountry: Map<String, WatchProviders>) : ProvidersState {
        fun forCountry(code: String): WatchProviders = byCountry[code] ?: WatchProviders.EMPTY
    }

    data object Failed : ProvidersState
}

sealed interface SearchEvent {
    data object Cleared : SearchEvent
    data object Started : SearchEvent
    data class MoviesLoaded(val query: String, val movies: List<Movie>, val hasMore: Boolean = false) : SearchEvent
    data class Failed(val error: Throwable) : SearchEvent
    data object NextPageLoading : SearchEvent
    data class NextPageLoaded(val movies: List<Movie>, val hasMore: Boolean) : SearchEvent
    data class NextPageFailed(val error: Throwable) : SearchEvent
    data class ProvidersLoaded(val movieId: Int, val result: Result<Map<String, WatchProviders>>) : SearchEvent
}

fun reduce(state: SearchUiState, event: SearchEvent): SearchUiState = when (event) {
    SearchEvent.Cleared -> SearchUiState.Idle

    SearchEvent.Started -> SearchUiState.Loading

    is SearchEvent.MoviesLoaded ->
        if (event.movies.isEmpty()) {
            SearchUiState.Empty(event.query)
        } else {
            SearchUiState.Results(
                movies = event.movies,
                providers = event.movies.associate { it.id to ProvidersState.Loading },
                nextPage = if (event.hasMore) NextPageState.Idle else NextPageState.End,
            )
        }

    is SearchEvent.Failed -> SearchUiState.Error(event.error)

    SearchEvent.NextPageLoading ->
        if (state is SearchUiState.Results) state.copy(nextPage = NextPageState.Loading) else state

    is SearchEvent.NextPageLoaded ->
        if (state is SearchUiState.Results) {
            val knownIds = state.movies.mapTo(HashSet()) { it.id }
            val newMovies = event.movies.filter { it.id !in knownIds }.distinctBy { it.id }
            state.copy(
                movies = state.movies + newMovies,
                providers = state.providers + newMovies.associate { it.id to ProvidersState.Loading },
                nextPage = if (event.hasMore) NextPageState.Idle else NextPageState.End,
            )
        } else {
            state
        }

    is SearchEvent.NextPageFailed ->
        if (state is SearchUiState.Results) state.copy(nextPage = NextPageState.Failed(event.error)) else state

    is SearchEvent.ProvidersLoaded ->
        if (state is SearchUiState.Results) {
            val providersState = event.result.fold(
                onSuccess = { ProvidersState.Loaded(it) },
                onFailure = { ProvidersState.Failed },
            )
            state.copy(providers = state.providers + (event.movieId to providersState))
        } else {
            state
        }
}
