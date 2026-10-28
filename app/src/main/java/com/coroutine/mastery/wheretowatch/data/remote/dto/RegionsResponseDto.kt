package com.coroutine.mastery.wheretowatch.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegionsResponseDto(
    val results: List<RegionDto> = emptyList(),
)

@Serializable
data class RegionDto(
    @SerialName("iso_3166_1") val code: String,
    @SerialName("english_name") val englishName: String,
)
