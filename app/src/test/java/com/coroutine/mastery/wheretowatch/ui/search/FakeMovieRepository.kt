package com.coroutine.mastery.wheretowatch.ui.search

import com.coroutine.mastery.wheretowatch.domain.model.CastMember
import com.coroutine.mastery.wheretowatch.domain.model.Movie
import com.coroutine.mastery.wheretowatch.domain.model.MovieDetails
import com.coroutine.mastery.wheretowatch.domain.model.MoviePage
import com.coroutine.mastery.wheretowatch.domain.model.Trailer
import com.coroutine.mastery.wheretowatch.domain.model.WatchProviders
import com.coroutine.mastery.wheretowatch.domain.repository.MovieRepository
import com.coroutine.mastery.wheretowatch.movie
import com.coroutine.mastery.wheretowatch.movieDetails
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

class FakeMovieRepository : MovieRepository {

    var searchDelayMillis = 100L
    var providersDelayMillis = 50L

    var searchResult: (query: String) -> Result<List<Movie>> = { Result.success(emptyList()) }
    var providersResult: (movieId: Int) -> Result<Map<String, WatchProviders>> = { Result.success(emptyMap()) }

    val searchQueries = mutableListOf<String>()
    val providerRequests = mutableListOf<Int>()
    val cancelledProviderRequests = mutableListOf<Int>()

    var totalPages = 1
    var nextPageDelayMillis = 100L
    var nextPageResult: (page: Int) -> Result<List<Movie>> = { page -> Result.success(listOf(movie(page * 100))) }
    val searchRequests = mutableListOf<Pair<String, Int>>()
    val cancelledPages = mutableListOf<Int>()

    override suspend fun searchMovies(query: String, page: Int): Result<MoviePage> {
        searchRequests += query to page
        if (page == 1) {
            searchQueries += query
            delay(searchDelayMillis)
            return searchResult(query).map { MoviePage(it, page = 1, totalPages = totalPages) }
        }
        try {
            delay(nextPageDelayMillis)
        } catch (e: CancellationException) {
            cancelledPages += page
            throw e
        }
        return nextPageResult(page).map { MoviePage(it, page = page, totalPages = totalPages) }
    }

    override suspend fun getWatchProviders(movieId: Int): Result<Map<String, WatchProviders>> {
        providerRequests += movieId
        try {
            delay(providersDelayMillis)
        } catch (e: CancellationException) {
            cancelledProviderRequests += movieId
            throw e
        }
        return providersResult(movieId)
    }

    var detailsDelayMillis = 100L
    var castDelayMillis = 300L
    var trailerDelayMillis = 200L
    var recommendationsDelayMillis = 250L

    var detailsResult: (movieId: Int) -> Result<MovieDetails> = { Result.success(movieDetails(it)) }
    var castResult: () -> Result<List<CastMember>> = { Result.success(listOf(CastMember(1, "Actor", "Hero", null))) }
    var trailerResult: () -> Result<Trailer?> = { Result.success(Trailer("Trailer", "https://youtube.com/watch?v=1")) }
    var recommendationsResult: () -> Result<List<Movie>> = { Result.success(emptyList()) }

    val detailsRequests = mutableListOf<Int>()
    val cancelledSections = mutableListOf<String>()

    override suspend fun getMovieDetails(movieId: Int): Result<MovieDetails> {
        detailsRequests += movieId
        trackedDelay("details", detailsDelayMillis)
        return detailsResult(movieId)
    }

    override suspend fun getCast(movieId: Int): Result<List<CastMember>> {
        trackedDelay("cast", castDelayMillis)
        return castResult()
    }

    override suspend fun getTrailer(movieId: Int): Result<Trailer?> {
        trackedDelay("trailer", trailerDelayMillis)
        return trailerResult()
    }

    override suspend fun getRecommendations(movieId: Int): Result<List<Movie>> {
        trackedDelay("recommendations", recommendationsDelayMillis)
        return recommendationsResult()
    }

    private suspend fun trackedDelay(section: String, millis: Long) {
        try {
            delay(millis)
        } catch (e: CancellationException) {
            cancelledSections += section
            throw e
        }
    }
}
