package com.coroutine.mastery.wheretowatch.data.remote

import com.coroutine.mastery.wheretowatch.data.remote.dto.CreditsDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.GenresResponseDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.MovieDetailsDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.RegionsResponseDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.SearchMoviesResponseDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.VideosDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.WatchProvidersResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApi {

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("language") language: String,
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
    ): SearchMoviesResponseDto

    @GET("genre/movie/list")
    suspend fun getMovieGenres(
        @Query("language") language: String,
    ): GenresResponseDto

    @GET("watch/providers/regions")
    suspend fun getWatchProviderRegions(
        @Query("language") language: String,
    ): RegionsResponseDto

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String,
    ): MovieDetailsDto

    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String,
    ): CreditsDto

    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String,
    ): VideosDto

    @GET("movie/{movie_id}/recommendations")
    suspend fun getMovieRecommendations(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String,
        @Query("page") page: Int = 1,
    ): SearchMoviesResponseDto

    @GET("movie/{movie_id}/watch/providers")
    suspend fun getWatchProviders(
        @Path("movie_id") movieId: Int,
    ): WatchProvidersResponseDto
}
