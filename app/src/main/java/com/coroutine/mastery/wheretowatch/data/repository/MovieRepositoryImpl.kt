package com.coroutine.mastery.wheretowatch.data.repository

import com.coroutine.mastery.wheretowatch.core.di.IoDispatcher
import com.coroutine.mastery.wheretowatch.core.util.suspendRunCatching
import com.coroutine.mastery.wheretowatch.data.mapper.mapFailureToAppError
import com.coroutine.mastery.wheretowatch.data.mapper.toDomain
import com.coroutine.mastery.wheretowatch.data.mapper.toTrailer
import com.coroutine.mastery.wheretowatch.data.remote.TmdbApi
import com.coroutine.mastery.wheretowatch.data.remote.TmdbConfig
import com.coroutine.mastery.wheretowatch.data.remote.dto.SearchMoviesResponseDto
import com.coroutine.mastery.wheretowatch.domain.model.CastMember
import com.coroutine.mastery.wheretowatch.domain.model.Movie
import com.coroutine.mastery.wheretowatch.domain.model.MovieDetails
import com.coroutine.mastery.wheretowatch.domain.model.MoviePage
import com.coroutine.mastery.wheretowatch.domain.model.Trailer
import com.coroutine.mastery.wheretowatch.domain.model.WatchProviders
import com.coroutine.mastery.wheretowatch.domain.repository.MovieRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class MovieRepositoryImpl @Inject constructor(
    private val api: TmdbApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : MovieRepository {

    private val genresMutex = Mutex()
    private var genresCache: Map<Int, String>? = null

    private val providersMutex = Mutex()
    private val providersCache = mutableMapOf<Int, Map<String, WatchProviders>>()

    override suspend fun searchMovies(query: String, page: Int): Result<MoviePage> =
        moviePage { api.searchMovies(query, TmdbConfig.LANGUAGE, page = page) }

    override suspend fun getRecommendations(movieId: Int): Result<List<Movie>> =
        moviePage { api.getMovieRecommendations(movieId, TmdbConfig.LANGUAGE) }.map { it.movies }

    override suspend fun getMovieDetails(movieId: Int): Result<MovieDetails> =
        request { api.getMovieDetails(movieId, TmdbConfig.LANGUAGE).toDomain() }

    override suspend fun getCast(movieId: Int): Result<List<CastMember>> =
        request { api.getMovieCredits(movieId, TmdbConfig.LANGUAGE).cast.toDomain().take(MAX_CAST) }

    override suspend fun getTrailer(movieId: Int): Result<Trailer?> =
        request { api.getMovieVideos(movieId, TmdbConfig.LANGUAGE).results.toTrailer() }

    override suspend fun getWatchProviders(movieId: Int): Result<Map<String, WatchProviders>> {
        providersMutex.withLock { providersCache[movieId] }?.let { return Result.success(it) }
        return request { api.getWatchProviders(movieId).toDomain() }
            .onSuccess { providers -> providersMutex.withLock { providersCache[movieId] = providers } }
    }

    private suspend fun <T> request(block: suspend () -> T): Result<T> =
        withContext(ioDispatcher) {
            suspendRunCatching { block() }.mapFailureToAppError()
        }

    private suspend fun moviePage(block: suspend () -> SearchMoviesResponseDto): Result<MoviePage> =
        request {
            coroutineScope {
                val genres = async { genreNames() }
                val response = block()
                val genreNames = genres.await()
                MoviePage(
                    movies = response.results.map { it.toDomain(genreNames) },
                    page = response.page,
                    totalPages = response.totalPages,
                )
            }
        }

    private suspend fun genreNames(): Map<Int, String> = genresMutex.withLock {
        genresCache ?: suspendRunCatching {
            api.getMovieGenres(TmdbConfig.LANGUAGE).genres.associate { it.id to it.name }
        }
            .onSuccess { genresCache = it }
            .getOrDefault(emptyMap())
    }

    private companion object {
        const val MAX_CAST = 15
    }
}
