package com.coroutine.mastery.wheretowatch.data.location

import com.coroutine.mastery.wheretowatch.data.location.AndroidCountryDetector.Companion.LOCATION_TIMEOUT_MILLIS
import com.coroutine.mastery.wheretowatch.domain.model.RegionSource
import com.coroutine.mastery.wheretowatch.domain.model.ResolvedRegion
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidCountryDetectorTest {

    private val bucharest = Coordinates(44.43, 26.10)

    private var location: suspend () -> Coordinates? = { bucharest }
    private var geocoder: suspend (Coordinates) -> String? = { "ro" }
    private var network: String? = "de"
    private var locale: String? = "FR"

    private val detector = AndroidCountryDetector(
        locationSource = { location() },
        geocoderSource = { geocoder(it) },
        networkCountrySource = { network },
        deviceLocaleSource = { locale },
    )

    @Test
    fun `uses the location when it can be geocoded`() = runTest {
        assertEquals(ResolvedRegion("RO", RegionSource.Location), detector.detect())
    }

    @Test
    fun `falls back to the network country without a location`() = runTest {
        location = { null }

        assertEquals(ResolvedRegion("DE", RegionSource.Network), detector.detect())
    }

    @Test
    fun `falls back to the network country when geocoding fails`() = runTest {
        geocoder = { throw IOException("service not available") }

        assertEquals(ResolvedRegion("DE", RegionSource.Network), detector.detect())
    }

    @Test
    fun `falls back to the device locale without a mobile network`() = runTest {
        location = { null }
        network = ""

        assertEquals(ResolvedRegion("FR", RegionSource.DeviceLocale), detector.detect())
    }

    @Test
    fun `falls back to the default country when nothing is known`() = runTest {
        location = { null }
        network = null
        locale = ""

        assertEquals(ResolvedRegion("US", RegionSource.Default), detector.detect())
    }

    @Test
    fun `a slow location gives up after the timeout and cancels the request`() = runTest {
        var cancelled = false
        location = {
            try {
                delay(60_000)
                bucharest
            } catch (e: CancellationException) {
                cancelled = true
                throw e
            }
        }

        val region = detector.detect()

        assertEquals(ResolvedRegion("DE", RegionSource.Network), region)
        assertEquals(LOCATION_TIMEOUT_MILLIS, testScheduler.currentTime)
        assertTrue(cancelled)
    }
}
