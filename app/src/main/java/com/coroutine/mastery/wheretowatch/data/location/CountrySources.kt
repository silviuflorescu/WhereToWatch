package com.coroutine.mastery.wheretowatch.data.location

data class Coordinates(val latitude: Double, val longitude: Double)

fun interface LocationSource {
    suspend fun coordinates(): Coordinates?
}

fun interface GeocoderSource {
    suspend fun countryCode(coordinates: Coordinates): String?
}

fun interface NetworkCountrySource {
    fun countryCode(): String?
}

fun interface DeviceLocaleSource {
    fun countryCode(): String?
}
