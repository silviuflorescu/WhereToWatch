package com.coroutine.mastery.wheretowatch.domain.model

data class Movie(
    val id: Int,
    val title: String,
    val year: Int?,
    val genres: List<String>,
    val overview: String,
    val posterUrl: String?,
    val rating: Double,
    val voteCount: Int,
)
