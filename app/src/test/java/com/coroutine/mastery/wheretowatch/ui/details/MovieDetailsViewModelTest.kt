package com.coroutine.mastery.wheretowatch.ui.details

import androidx.lifecycle.SavedStateHandle
import com.coroutine.mastery.wheretowatch.MainDispatcherRule
import com.coroutine.mastery.wheretowatch.domain.model.AppError
import com.coroutine.mastery.wheretowatch.domain.usecase.GetCastUseCase
import com.coroutine.mastery.wheretowatch.domain.usecase.GetMovieDetailsUseCase
import com.coroutine.mastery.wheretowatch.domain.usecase.GetRecommendationsUseCase
import com.coroutine.mastery.wheretowatch.domain.usecase.GetTrailerUseCase
import com.coroutine.mastery.wheretowatch.domain.usecase.GetWatchProvidersUseCase
import com.coroutine.mastery.wheretowatch.movieDetails
import com.coroutine.mastery.wheretowatch.ui.search.FakeMovieRepository
import com.coroutine.mastery.wheretowatch.ui.search.ProvidersState
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
class MovieDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeMovieRepository().apply {
        providersResult = { Result.success(usProviders("Netflix")) }
    }
    private fun viewModel() = MovieDetailsViewModel(
        savedStateHandle = SavedStateHandle(mapOf("movieId" to 27205)),
        getMovieDetails = GetMovieDetailsUseCase(repository),
        getCast = GetCastUseCase(repository),
        getTrailer = GetTrailerUseCase(repository),
        getRecommendations = GetRecommendationsUseCase(repository),
        getWatchProviders = GetWatchProvidersUseCase(repository),
    )

    private fun TestScope.collect(viewModel: MovieDetailsViewModel): Job =
        backgroundScope.launch { viewModel.uiState.collect {} }.also { runCurrent() }

    @Test
    fun `details show up before the slower sections finish`() = runTest {
        val viewModel = viewModel()
        collect(viewModel)

        advanceTimeBy(repository.detailsDelayMillis + 1)

        val state = viewModel.uiState.value
        assertEquals(Section.Loaded(movieDetails(27205)), state.details)
        assertTrue(state.providers is ProvidersState.Loaded)
        assertEquals(Section.Loading, state.cast)
        assertEquals(Section.Loading, state.recommendations)
    }

    @Test
    fun `all requests run in parallel`() = runTest {
        val viewModel = viewModel()
        collect(viewModel)

        advanceUntilIdle()

        assertEquals(repository.castDelayMillis, testScheduler.currentTime)
        val state = viewModel.uiState.value
        assertTrue(state.cast is Section.Loaded)
        assertTrue(state.trailer is Section.Loaded)
        assertTrue(state.recommendations is Section.Loaded)
    }

    @Test
    fun `a failed section doesn't affect the others`() = runTest {
        repository.castResult = { Result.failure(AppError.Network) }
        val viewModel = viewModel()
        collect(viewModel)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(Section.Failed(AppError.Network), state.cast)
        assertTrue(state.details is Section.Loaded)
        assertTrue(state.trailer is Section.Loaded)
        assertTrue(state.recommendations is Section.Loaded)
        assertEquals(ProvidersState.Loaded(usProviders("Netflix")), state.providers)
    }

    @Test
    fun `details failure shows an error and retry loads again`() = runTest {
        var attempt = 0
        repository.detailsResult = { id ->
            if (attempt++ == 0) Result.failure(AppError.Network) else Result.success(movieDetails(id))
        }
        val viewModel = viewModel()
        collect(viewModel)
        advanceUntilIdle()
        assertEquals(Section.Failed(AppError.Network), viewModel.uiState.value.details)

        viewModel.onRetry()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.details is Section.Loaded)
        assertEquals(listOf(27205, 27205), repository.detailsRequests)
    }

    @Test
    fun `leaving the screen cancels the requests still running`() = runTest {
        repository.castDelayMillis = 60_000
        val viewModel = viewModel()
        val collector = collect(viewModel)
        advanceTimeBy(1_000)

        collector.cancel()
        advanceTimeBy(5_001)

        assertEquals(listOf("cast"), repository.cancelledSections)
    }
}
