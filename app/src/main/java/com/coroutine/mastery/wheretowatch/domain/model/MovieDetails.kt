package com.coroutine.mastery.wheretowatch.domain.model

data class MovieDetails(
    val id: Int,
    val title: String,
    val tagline: String?,
    val overview: String,
    val year: Int?,
    val runtimeMinutes: Int?,
    val genres: List<String>,
    val rating: Double,
    val voteCount: Int,
    val posterUrl: String?,
    val backdropUrl: String?,
)

data class CastMember(
    val id: Int,
    val name: String,
    val character: String?,
    val photoUrl: String?,
)

data class Trailer(
    val name: String,
    val url: String,
)
