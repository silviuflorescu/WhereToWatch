package com.coroutine.mastery.wheretowatch.data.mapper

import com.coroutine.mastery.wheretowatch.data.remote.TmdbConfig
import com.coroutine.mastery.wheretowatch.data.remote.dto.CastDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.CountryProvidersDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.MovieDetailsDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.MovieDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.ProviderDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.VideoDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.WatchProvidersResponseDto
import com.coroutine.mastery.wheretowatch.domain.model.CastMember
import com.coroutine.mastery.wheretowatch.domain.model.Movie
import com.coroutine.mastery.wheretowatch.domain.model.MovieDetails
import com.coroutine.mastery.wheretowatch.domain.model.Provider
import com.coroutine.mastery.wheretowatch.domain.model.Trailer
import com.coroutine.mastery.wheretowatch.domain.model.WatchProviders

fun MovieDto.toDomain(genreNames: Map<Int, String>): Movie = Movie(
    id = id,
    title = title,
    year = releaseDate?.take(4)?.toIntOrNull(),
    genres = genreIds.mapNotNull(genreNames::get),
    overview = overview,
    posterUrl = posterPath.toImageUrl(TmdbConfig.POSTER_SIZE),
    rating = voteAverage,
    voteCount = voteCount,
)

fun MovieDetailsDto.toDomain(): MovieDetails = MovieDetails(
    id = id,
    title = title,
    tagline = tagline?.takeIf { it.isNotBlank() },
    overview = overview,
    year = releaseDate?.take(4)?.toIntOrNull(),
    runtimeMinutes = runtime?.takeIf { it > 0 },
    genres = genres.map { it.name },
    rating = voteAverage,
    voteCount = voteCount,
    posterUrl = posterPath.toImageUrl(TmdbConfig.POSTER_SIZE),
    backdropUrl = backdropPath.toImageUrl(TmdbConfig.BACKDROP_SIZE),
)

fun List<CastDto>.toDomain(): List<CastMember> =
    sortedBy { it.order }.map {
        CastMember(
            id = it.id,
            name = it.name,
            character = it.character?.takeIf(String::isNotBlank),
            photoUrl = it.profilePath.toImageUrl(TmdbConfig.PROFILE_SIZE),
        )
    }

fun List<VideoDto>.toTrailer(): Trailer? =
    filter { it.site == "YouTube" }
        .sortedWith(compareBy({ it.type != "Trailer" }, { !it.official }))
        .firstOrNull { it.type == "Trailer" || it.type == "Teaser" }
        ?.let { Trailer(name = it.name, url = "https://www.youtube.com/watch?v=${it.key}") }

fun WatchProvidersResponseDto.toDomain(): Map<String, WatchProviders> =
    results.mapValues { (_, providers) -> providers.toDomain() }

private fun CountryProvidersDto.toDomain(): WatchProviders = WatchProviders(
    stream = (flatrate + free + ads).toProviders(),
    rent = rent.toProviders(),
    buy = buy.toProviders(),
    link = link,
)

private fun List<ProviderDto>.toProviders(): List<Provider> =
    sortedBy { it.displayPriority }
        .distinctBy { it.providerId }
        .map { Provider(it.providerId, it.providerName, it.logoPath.toImageUrl(TmdbConfig.LOGO_SIZE)) }

private fun String?.toImageUrl(size: String): String? =
    this?.let { "${TmdbConfig.IMAGE_BASE_URL}$size$it" }
