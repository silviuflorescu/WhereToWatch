package com.coroutine.mastery.wheretowatch.data.repository

import com.coroutine.mastery.wheretowatch.data.remote.dto.CastDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.CountryProvidersDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.CreditsDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.GenreDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.GenresResponseDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.MovieDetailsDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.MovieDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.ProviderDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.SearchMoviesResponseDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.VideoDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.VideosDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.WatchProvidersResponseDto
import com.coroutine.mastery.wheretowatch.domain.model.AppError
import com.coroutine.mastery.wheretowatch.domain.model.Movie
import com.coroutine.mastery.wheretowatch.domain.model.MoviePage
import com.coroutine.mastery.wheretowatch.domain.model.WatchProviders
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class MovieRepositoryImplTest {

    private val api = FakeTmdbApi()

    private fun TestScope.repository() = MovieRepositoryImpl(api, StandardTestDispatcher(testScheduler))

    private val inceptionDto = MovieDto(
        id = 27205,
        title = "Inception",
        overview = "Dreams.",
        releaseDate = "2010-07-15",
        posterPath = "/poster.jpg",
        genreIds = listOf(28, 878, 999),
        voteAverage = 8.4,
        voteCount = 37000,
    )

    @Test
    fun `search maps movies with genre names, year and poster url`() = runTest {
        api.searchResponse = { SearchMoviesResponseDto(results = listOf(inceptionDto)) }
        api.genresResponse = { GenresResponseDto(listOf(GenreDto(28, "Action"), GenreDto(878, "Science Fiction"))) }

        val movies = repository().searchMovies("inception", page = 1).getOrThrow().movies

        assertEquals(
            listOf(
                Movie(
                    id = 27205,
                    title = "Inception",
                    year = 2010,
                    genres = listOf("Action", "Science Fiction"),
                    overview = "Dreams.",
                    posterUrl = "https://image.tmdb.org/t/p/w342/poster.jpg",
                    rating = 8.4,
                    voteCount = 37000,
                )
            ),
            movies,
        )
    }

    @Test
    fun `search and genres are fetched in parallel`() = runTest {
        api.searchResponse = { SearchMoviesResponseDto(results = listOf(inceptionDto)) }

        repository().searchMovies("inception", page = 1)

        assertEquals(100, testScheduler.currentTime)
    }

    @Test
    fun `genres are fetched only once, even for concurrent searches`() = runTest {
        val repository = repository()

        val first = async { repository.searchMovies("a", page = 1) }
        val second = async { repository.searchMovies("b", page = 1) }
        first.await()
        second.await()
        repository.searchMovies("c", page = 1)

        assertEquals(1, api.genresCalls)
    }

    @Test
    fun `genres failure does not fail the search and is retried next time`() = runTest {
        api.searchResponse = { SearchMoviesResponseDto(results = listOf(inceptionDto)) }
        api.genresResponse = { throw IOException("offline") }
        val repository = repository()

        val movies = repository.searchMovies("inception", page = 1).getOrThrow().movies
        assertTrue(movies.single().genres.isEmpty())

        api.genresResponse = { GenresResponseDto(listOf(GenreDto(28, "Action"))) }
        val retried = repository.searchMovies("inception", page = 1).getOrThrow().movies
        assertEquals(listOf("Action"), retried.single().genres)
        assertEquals(2, api.genresCalls)
    }

    @Test
    fun `network failure is mapped to AppError Network`() = runTest {
        api.searchResponse = { throw IOException("offline") }

        val result = repository().searchMovies("inception", page = 1)

        assertEquals(AppError.Network, result.exceptionOrNull())
    }

    @Test
    fun `http 401 is mapped to AppError Unauthorized`() = runTest {
        api.searchResponse = { throw HttpException(Response.error<Any>(401, "".toResponseBody())) }

        val result = repository().searchMovies("inception", page = 1)

        assertEquals(AppError.Unauthorized, result.exceptionOrNull())
    }

    @Test
    fun `cancellation is propagated, not turned into a failure`() = runTest {
        var result: Result<MoviePage>? = null
        val job = launch { result = repository().searchMovies("inception", page = 1) }

        advanceTimeBy(50)
        job.cancel()
        advanceUntilIdle()

        assertTrue(job.isCancelled)
        assertNull(result)
    }

    @Test
    fun `watch providers are returned for every country and merge streaming options`() = runTest {
        fun provider(id: Int, name: String, priority: Int) =
            ProviderDto(providerId = id, providerName = name, logoPath = "/$id.png", displayPriority = priority)
        api.providersResponse = { id ->
            WatchProvidersResponseDto(
                id = id,
                results = mapOf(
                    "US" to CountryProvidersDto(
                        link = "https://tmdb.org/movie/1/watch",
                        flatrate = listOf(provider(8, "Netflix", priority = 2)),
                        free = listOf(provider(73, "Tubi", priority = 1)),
                        rent = listOf(provider(2, "Apple TV", priority = 1)),
                    ),
                    "RO" to CountryProvidersDto(flatrate = listOf(provider(1899, "Max", priority = 1))),
                ),
            )
        }

        val byCountry = repository().getWatchProviders(1).getOrThrow()
        val us = byCountry.getValue("US")
        val ro = byCountry.getValue("RO")

        assertEquals(listOf("Tubi", "Netflix"), us.stream.map { it.name })
        assertEquals(listOf("Apple TV"), us.rent.map { it.name })
        assertEquals("https://image.tmdb.org/t/p/w92/8.png", us.stream[1].logoUrl)
        assertEquals("https://tmdb.org/movie/1/watch", us.link)
        assertEquals(listOf("Max"), ro.stream.map { it.name })
    }

    @Test
    fun `countries without any provider are present but empty`() = runTest {
        api.providersResponse = { id ->
            WatchProvidersResponseDto(id = id, results = mapOf("RO" to CountryProvidersDto()))
        }

        val byCountry = repository().getWatchProviders(1).getOrThrow()

        assertEquals(mapOf("RO" to WatchProviders.EMPTY), byCountry)
    }

    @Test
    fun `watch providers are cached, so the details screen doesn't request them again`() = runTest {
        val repository = repository()

        repository.getWatchProviders(1)
        repository.getWatchProviders(1)
        repository.getWatchProviders(2)

        assertEquals(2, api.providersCalls)
    }

    @Test
    fun `details are mapped with runtime, genres and images`() = runTest {
        api.detailsResponse = { id ->
            MovieDetailsDto(
                id = id,
                title = "Inception",
                tagline = "",
                releaseDate = "2010-07-15",
                runtime = 148,
                genres = listOf(GenreDto(28, "Action")),
                posterPath = "/p.jpg",
                backdropPath = "/b.jpg",
            )
        }

        val details = repository().getMovieDetails(27205).getOrThrow()

        assertEquals(2010, details.year)
        assertEquals(148, details.runtimeMinutes)
        assertEquals(listOf("Action"), details.genres)
        assertNull(details.tagline)
        assertEquals("https://image.tmdb.org/t/p/w780/b.jpg", details.backdropUrl)
    }

    @Test
    fun `cast is in billing order and limited`() = runTest {
        api.creditsResponse = {
            CreditsDto((1..20).reversed().map { CastDto(id = it, name = "Actor $it", order = it) })
        }

        val cast = repository().getCast(1).getOrThrow()

        assertEquals(15, cast.size)
        assertEquals("Actor 1", cast.first().name)
    }

    @Test
    fun `trailer prefers an official YouTube trailer`() = runTest {
        api.videosResponse = {
            VideosDto(
                listOf(
                    VideoDto(key = "teaser", site = "YouTube", type = "Teaser", official = true),
                    VideoDto(key = "vimeo", site = "Vimeo", type = "Trailer", official = true),
                    VideoDto(key = "fan", site = "YouTube", type = "Trailer", official = false),
                    VideoDto(key = "official", site = "YouTube", type = "Trailer", official = true),
                )
            )
        }

        val trailer = repository().getTrailer(1).getOrThrow()

        assertEquals("https://www.youtube.com/watch?v=official", trailer?.url)
    }

    @Test
    fun `no trailer is not an error`() = runTest {
        assertNull(repository().getTrailer(1).getOrThrow())
    }

    @Test
    fun `recommendations get genre names like the search`() = runTest {
        api.recommendationsResponse = { SearchMoviesResponseDto(results = listOf(inceptionDto)) }
        api.genresResponse = { GenresResponseDto(listOf(GenreDto(28, "Action"))) }

        val movies = repository().getRecommendations(1).getOrThrow()

        assertEquals(listOf("Action"), movies.single().genres)
    }

    @Test
    fun `search asks for the requested page and knows if more pages exist`() = runTest {
        api.searchResponse = { SearchMoviesResponseDto(page = 2, results = listOf(inceptionDto), totalPages = 3) }
        val repository = repository()

        val page = repository.searchMovies("inception", page = 2).getOrThrow()

        assertEquals(listOf(2), api.searchPages)
        assertEquals(2, page.page)
        assertTrue(page.hasMore)
    }
}
