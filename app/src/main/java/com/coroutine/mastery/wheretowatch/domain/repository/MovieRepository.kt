package com.coroutine.mastery.wheretowatch.domain.repository

import com.coroutine.mastery.wheretowatch.domain.model.CastMember
import com.coroutine.mastery.wheretowatch.domain.model.Movie
import com.coroutine.mastery.wheretowatch.domain.model.MovieDetails
import com.coroutine.mastery.wheretowatch.domain.model.MoviePage
import com.coroutine.mastery.wheretowatch.domain.model.Trailer
import com.coroutine.mastery.wheretowatch.domain.model.WatchProviders

interface MovieRepository {
    suspend fun searchMovies(query: String, page: Int): Result<MoviePage>

    suspend fun getMovieDetails(movieId: Int): Result<MovieDetails>

    suspend fun getCast(movieId: Int): Result<List<CastMember>>

    suspend fun getTrailer(movieId: Int): Result<Trailer?>

    suspend fun getRecommendations(movieId: Int): Result<List<Movie>>

    suspend fun getWatchProviders(movieId: Int): Result<Map<String, WatchProviders>>
}
