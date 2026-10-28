package com.coroutine.mastery.wheretowatch.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GenresResponseDto(
    val genres: List<GenreDto> = emptyList(),
)

@Serializable
data class GenreDto(
    val id: Int,
    val name: String,
)
