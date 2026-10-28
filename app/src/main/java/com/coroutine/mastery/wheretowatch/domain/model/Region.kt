package com.coroutine.mastery.wheretowatch.domain.model

sealed interface RegionSelection {
    data object Auto : RegionSelection

    data class Manual(val code: String) : RegionSelection
}

data class ResolvedRegion(
    val code: String,
    val source: RegionSource,
)

enum class RegionSource {
    Manual,
    Location,
    Network,
    DeviceLocale,
    Default,
}
