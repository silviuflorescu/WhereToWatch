package com.coroutine.mastery.wheretowatch.ui.search

import app.cash.turbine.test
import com.coroutine.mastery.wheretowatch.MainDispatcherRule
import com.coroutine.mastery.wheretowatch.domain.model.AppError
import com.coroutine.mastery.wheretowatch.domain.usecase.GetWatchProvidersUseCase
import com.coroutine.mastery.wheretowatch.domain.usecase.SearchMoviesUseCase
import com.coroutine.mastery.wheretowatch.movie
import com.coroutine.mastery.wheretowatch.ui.search.SearchViewModel.Companion.DEBOUNCE_MILLIS
import com.coroutine.mastery.wheretowatch.ui.search.SearchViewModel.Companion.MAX_CONCURRENT_PROVIDER_REQUESTS
import com.coroutine.mastery.wheretowatch.usProviders
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
class SearchViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMovieRepository()

    private val viewModel by lazy {
        SearchViewModel(
            searchMovies = SearchMoviesUseCase(repository),
            getWatchProviders = GetWatchProvidersUseCase(repository),
        )
    }

    private fun TestScope.collectUiState(): Job =
        backgroundScope.launch { viewModel.uiState.collect {} }.also { runCurrent() }

    @Test
    fun `typing quickly triggers a single search after the debounce`() = runTest {
        collectUiState()

        viewModel.onQueryChange("in")
        advanceTimeBy(100)
        viewModel.onQueryChange("ince")
        advanceTimeBy(100)
        viewModel.onQueryChange("incep")

        advanceTimeBy(DEBOUNCE_MILLIS - 1)
        assertTrue(repository.searchQueries.isEmpty())

        advanceTimeBy(2)
        assertEquals(listOf("incep"), repository.searchQueries)
    }

    @Test
    fun `emits Loading, then movies, then their watch providers`() = runTest {
        repository.searchResult = { Result.success(listOf(movie(1), movie(2))) }
        repository.providersResult = { Result.success(usProviders("Netflix")) }

        viewModel.uiState.test {
            assertEquals(SearchUiState.Idle, awaitItem())

            viewModel.onQueryChange("inception")
            assertEquals(SearchUiState.Loading, awaitItem())

            val results = awaitItem() as SearchUiState.Results
            assertEquals(listOf(1, 2), results.movies.map { it.id })
            assertTrue(results.providers.values.all { it == ProvidersState.Loading })

            advanceUntilIdle()
            val withProviders = expectMostRecentItem() as SearchUiState.Results
            assertEquals(
                mapOf(
                    1 to ProvidersState.Loaded(usProviders("Netflix")),
                    2 to ProvidersState.Loaded(usProviders("Netflix")),
                ),
                withProviders.providers,
            )
        }
    }

    @Test
    fun `a new query cancels the provider requests of the previous one`() = runTest {
        repository.providersDelayMillis = 10_000
        repository.searchResult = { query ->
            Result.success(if (query == "first") listOf(movie(1), movie(2)) else listOf(movie(3)))
        }
        collectUiState()

        viewModel.onQueryChange("first")
        advanceTimeBy(DEBOUNCE_MILLIS + repository.searchDelayMillis + 1)
        assertEquals(listOf(1, 2), repository.providerRequests)

        viewModel.onQueryChange("second")
        advanceUntilIdle()

        assertEquals(listOf(1, 2), repository.cancelledProviderRequests)
        val state = viewModel.uiState.value as SearchUiState.Results
        assertEquals(listOf(3), state.movies.map { it.id })
        assertTrue(state.providers[3] is ProvidersState.Loaded)
    }

    @Test
    fun `provider failure for one movie does not affect the others`() = runTest {
        repository.searchResult = { Result.success(listOf(movie(1), movie(2))) }
        repository.providersResult = { id ->
            if (id == 1) Result.failure(AppError.Network) else Result.success(usProviders("Max"))
        }
        collectUiState()

        viewModel.onQueryChange("inception")
        advanceUntilIdle()

        val state = viewModel.uiState.value as SearchUiState.Results
        assertEquals(ProvidersState.Failed, state.providers[1])
        assertEquals(ProvidersState.Loaded(usProviders("Max")), state.providers[2])
    }

    @Test
    fun `provider requests are limited in parallel`() = runTest {
        repository.searchResult = { Result.success((1..10).map(::movie)) }
        collectUiState()

        viewModel.onQueryChange("inception")
        advanceTimeBy(DEBOUNCE_MILLIS + repository.searchDelayMillis + 1)

        assertEquals(MAX_CONCURRENT_PROVIDER_REQUESTS, repository.providerRequests.size)

        advanceUntilIdle()
        assertEquals(10, repository.providerRequests.size)
    }

    @Test
    fun `search error shows Error and retry recovers`() = runTest {
        var attempt = 0
        repository.searchResult = {
            if (attempt++ == 0) Result.failure(AppError.Network) else Result.success(listOf(movie(1)))
        }
        collectUiState()

        viewModel.onQueryChange("inception")
        advanceUntilIdle()
        assertEquals(SearchUiState.Error(AppError.Network), viewModel.uiState.value)

        viewModel.onRetry()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is SearchUiState.Results)
        assertEquals(listOf("inception", "inception"), repository.searchQueries)
    }

    @Test
    fun `no results shows Empty`() = runTest {
        collectUiState()

        viewModel.onQueryChange("asdfgh")
        advanceUntilIdle()

        assertEquals(SearchUiState.Empty("asdfgh"), viewModel.uiState.value)
    }

    @Test
    fun `short query stays Idle without searching`() = runTest {
        collectUiState()

        viewModel.onQueryChange("a")
        advanceUntilIdle()

        assertEquals(SearchUiState.Idle, viewModel.uiState.value)
        assertTrue(repository.searchQueries.isEmpty())
    }

    @Test
    fun `clearing the query goes back to Idle immediately`() = runTest {
        repository.searchResult = { Result.success(listOf(movie(1))) }
        collectUiState()
        viewModel.onQueryChange("inception")
        advanceUntilIdle()

        viewModel.onClearQuery()
        runCurrent()

        assertEquals(SearchUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun `coming back from another screen keeps the results without searching again`() = runTest {
        repository.searchResult = { Result.success(listOf(movie(1))) }
        val collector = collectUiState()
        viewModel.onQueryChange("inception")
        advanceUntilIdle()
        val results = viewModel.uiState.value

        collector.cancel()
        advanceTimeBy(60_000)
        collectUiState()
        advanceUntilIdle()

        assertEquals(results, viewModel.uiState.value)
        assertEquals(listOf("inception"), repository.searchQueries)
    }

    @Test
    fun `the next page is appended with its providers`() = runTest {
        repository.totalPages = 3
        repository.searchResult = { Result.success(listOf(movie(1), movie(2))) }
        repository.providersResult = { Result.success(usProviders("Netflix")) }
        collectUiState()
        viewModel.onQueryChange("inception")
        advanceUntilIdle()
        assertEquals(NextPageState.Idle, (viewModel.uiState.value as SearchUiState.Results).nextPage)

        viewModel.onLoadNextPage()
        advanceUntilIdle()

        val state = viewModel.uiState.value as SearchUiState.Results
        assertEquals(listOf(1, 2, 200), state.movies.map { it.id })
        assertEquals(ProvidersState.Loaded(usProviders("Netflix")), state.providers[200])
        assertEquals(NextPageState.Idle, state.nextPage)
        assertEquals(listOf("inception" to 1, "inception" to 2), repository.searchRequests)
    }

    @Test
    fun `asking for the next page while one is loading is ignored`() = runTest {
        repository.totalPages = 3
        repository.searchResult = { Result.success(listOf(movie(1))) }
        collectUiState()
        viewModel.onQueryChange("inception")
        advanceUntilIdle()

        viewModel.onLoadNextPage()
        runCurrent()
        assertEquals(NextPageState.Loading, (viewModel.uiState.value as SearchUiState.Results).nextPage)
        viewModel.onLoadNextPage()
        viewModel.onLoadNextPage()
        advanceUntilIdle()

        assertEquals(listOf(1, 2), repository.searchRequests.map { it.second })
    }

    @Test
    fun `nothing is requested after the last page`() = runTest {
        repository.totalPages = 2
        repository.searchResult = { Result.success(listOf(movie(1))) }
        collectUiState()
        viewModel.onQueryChange("inception")
        advanceUntilIdle()

        viewModel.onLoadNextPage()
        advanceUntilIdle()
        assertEquals(NextPageState.End, (viewModel.uiState.value as SearchUiState.Results).nextPage)
        viewModel.onLoadNextPage()
        advanceUntilIdle()

        assertEquals(listOf(1, 2), repository.searchRequests.map { it.second })
    }

    @Test
    fun `a failed page keeps the movies and can be retried`() = runTest {
        repository.totalPages = 3
        repository.searchResult = { Result.success(listOf(movie(1))) }
        var attempt = 0
        repository.nextPageResult = { page ->
            if (attempt++ == 0) Result.failure(AppError.Network) else Result.success(listOf(movie(page * 100)))
        }
        collectUiState()
        viewModel.onQueryChange("inception")
        advanceUntilIdle()

        viewModel.onLoadNextPage()
        advanceUntilIdle()
        val failed = viewModel.uiState.value as SearchUiState.Results
        assertEquals(NextPageState.Failed(AppError.Network), failed.nextPage)
        assertEquals(listOf(1), failed.movies.map { it.id })

        viewModel.onLoadNextPage()
        advanceUntilIdle()

        assertEquals(listOf(1, 200), (viewModel.uiState.value as SearchUiState.Results).movies.map { it.id })
        assertEquals(listOf(1, 2, 2), repository.searchRequests.map { it.second })
    }

    @Test
    fun `a new query cancels the page that is loading`() = runTest {
        repository.totalPages = 3
        repository.nextPageDelayMillis = 10_000
        repository.searchResult = { Result.success(listOf(movie(1))) }
        collectUiState()
        viewModel.onQueryChange("inception")
        advanceUntilIdle()
        viewModel.onLoadNextPage()
        runCurrent()

        viewModel.onQueryChange("interstellar")
        advanceTimeBy(DEBOUNCE_MILLIS + 1)

        assertEquals(listOf(2), repository.cancelledPages)
        advanceUntilIdle()
        assertEquals(NextPageState.Idle, (viewModel.uiState.value as SearchUiState.Results).nextPage)
    }
}
