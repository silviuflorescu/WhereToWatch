package com.coroutine.mastery.wheretowatch.data.repository

import com.coroutine.mastery.wheretowatch.data.remote.TmdbApi
import com.coroutine.mastery.wheretowatch.data.remote.dto.CreditsDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.GenresResponseDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.MovieDetailsDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.RegionsResponseDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.SearchMoviesResponseDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.VideosDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.WatchProvidersResponseDto
import kotlinx.coroutines.delay

class FakeTmdbApi : TmdbApi {

    var searchDelayMillis = 100L
    var genresDelayMillis = 100L

    var searchResponse: () -> SearchMoviesResponseDto = { SearchMoviesResponseDto() }
    var genresResponse: () -> GenresResponseDto = { GenresResponseDto() }
    var providersResponse: (Int) -> WatchProvidersResponseDto = { WatchProvidersResponseDto(id = it) }
    var regionsResponse: () -> RegionsResponseDto = { RegionsResponseDto() }

    var genresCalls = 0
        private set
    var regionsCalls = 0
        private set

    override suspend fun searchMovies(
        query: String,
        language: String,
        page: Int,
        includeAdult: Boolean,
    ): SearchMoviesResponseDto {
        searchPages += page
        delay(searchDelayMillis)
        return searchResponse()
    }

    val searchPages = mutableListOf<Int>()

    override suspend fun getMovieGenres(language: String): GenresResponseDto {
        genresCalls++
        delay(genresDelayMillis)
        return genresResponse()
    }

    override suspend fun getWatchProviderRegions(language: String): RegionsResponseDto {
        regionsCalls++
        delay(100)
        return regionsResponse()
    }

    var providersCalls = 0
        private set

    override suspend fun getWatchProviders(movieId: Int): WatchProvidersResponseDto {
        providersCalls++
        return providersResponse(movieId)
    }

    var detailsResponse: (Int) -> MovieDetailsDto = { MovieDetailsDto(id = it, title = "Movie $it") }
    var creditsResponse: () -> CreditsDto = { CreditsDto() }
    var videosResponse: () -> VideosDto = { VideosDto() }
    var recommendationsResponse: () -> SearchMoviesResponseDto = { SearchMoviesResponseDto() }

    override suspend fun getMovieDetails(movieId: Int, language: String): MovieDetailsDto = detailsResponse(movieId)

    override suspend fun getMovieCredits(movieId: Int, language: String): CreditsDto = creditsResponse()

    override suspend fun getMovieVideos(movieId: Int, language: String): VideosDto = videosResponse()

    override suspend fun getMovieRecommendations(movieId: Int, language: String, page: Int): SearchMoviesResponseDto =
        recommendationsResponse()
}
