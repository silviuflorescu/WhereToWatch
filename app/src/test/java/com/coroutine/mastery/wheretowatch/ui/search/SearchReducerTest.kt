package com.coroutine.mastery.wheretowatch.ui.search

import com.coroutine.mastery.wheretowatch.domain.model.AppError
import com.coroutine.mastery.wheretowatch.movie
import com.coroutine.mastery.wheretowatch.domain.model.WatchProviders
import com.coroutine.mastery.wheretowatch.usProviders
import com.coroutine.mastery.wheretowatch.watchProviders
import org.junit.Assert.assertEquals
import org.junit.Test

class SearchReducerTest {

    private val results = SearchUiState.Results(
        movies = listOf(movie(1), movie(2)),
        providers = mapOf(1 to ProvidersState.Loading, 2 to ProvidersState.Loading),
    )

    @Test
    fun `movies loaded starts every card with loading providers`() {
        val state = reduce(SearchUiState.Loading, SearchEvent.MoviesLoaded("q", listOf(movie(1), movie(2))))

        assertEquals(results, state)
    }

    @Test
    fun `no movies gives Empty with the query`() {
        val state = reduce(SearchUiState.Loading, SearchEvent.MoviesLoaded("q", emptyList()))

        assertEquals(SearchUiState.Empty("q"), state)
    }

    @Test
    fun `providers loaded updates only that movie`() {
        val state = reduce(results, SearchEvent.ProvidersLoaded(2, Result.success(usProviders("Netflix"))))

        assertEquals(
            mapOf(
                1 to ProvidersState.Loading,
                2 to ProvidersState.Loaded(usProviders("Netflix")),
            ),
            (state as SearchUiState.Results).providers,
        )
    }

    @Test
    fun `providers failure marks only that movie as failed`() {
        val state = reduce(results, SearchEvent.ProvidersLoaded(1, Result.failure(AppError.Network)))

        assertEquals(ProvidersState.Failed, (state as SearchUiState.Results).providers[1])
        assertEquals(ProvidersState.Loading, state.providers[2])
    }

    @Test
    fun `loaded providers pick the selected country`() {
        val loaded = ProvidersState.Loaded(mapOf("US" to watchProviders("Netflix"), "RO" to watchProviders("Max")))

        assertEquals(watchProviders("Max"), loaded.forCountry("RO"))
        assertEquals(WatchProviders.EMPTY, loaded.forCountry("DE"))
    }

    @Test
    fun `providers event is ignored when there are no results`() {
        val state = reduce(SearchUiState.Loading, SearchEvent.ProvidersLoaded(1, Result.success(usProviders())))

        assertEquals(SearchUiState.Loading, state)
    }

    @Test
    fun `started, failed and cleared map to their states`() {
        assertEquals(SearchUiState.Loading, reduce(results, SearchEvent.Started))
        assertEquals(SearchUiState.Error(AppError.Network), reduce(results, SearchEvent.Failed(AppError.Network)))
        assertEquals(SearchUiState.Idle, reduce(results, SearchEvent.Cleared))
    }

    @Test
    fun `the next page is appended without duplicates`() {
        val state = reduce(
            results.copy(nextPage = NextPageState.Loading),
            SearchEvent.NextPageLoaded(listOf(movie(2), movie(3), movie(3)), hasMore = false),
        ) as SearchUiState.Results

        assertEquals(listOf(1, 2, 3), state.movies.map { it.id })
        assertEquals(ProvidersState.Loading, state.providers[3])
        assertEquals(NextPageState.End, state.nextPage)
    }

    @Test
    fun `a failed page keeps the movies`() {
        val state = reduce(results, SearchEvent.NextPageFailed(AppError.Network)) as SearchUiState.Results

        assertEquals(results.movies, state.movies)
        assertEquals(NextPageState.Failed(AppError.Network), state.nextPage)
    }
}
