package com.coroutine.mastery.wheretowatch.data.repository

import com.coroutine.mastery.wheretowatch.data.remote.dto.RegionDto
import com.coroutine.mastery.wheretowatch.data.remote.dto.RegionsResponseDto
import com.coroutine.mastery.wheretowatch.domain.model.AppError
import com.coroutine.mastery.wheretowatch.domain.model.Country
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CountryRepositoryImplTest {

    private val api = FakeTmdbApi().apply {
        regionsResponse = {
            RegionsResponseDto(listOf(RegionDto("US", "United States of America"), RegionDto("DE", "Germany")))
        }
    }

    private fun TestScope.repository() = CountryRepositoryImpl(api, StandardTestDispatcher(testScheduler))

    @Test
    fun `countries are mapped and sorted by name`() = runTest {
        val countries = repository().getCountries().getOrThrow()

        assertEquals(listOf(Country("DE", "Germany"), Country("US", "United States of America")), countries)
    }

    @Test
    fun `countries are fetched only once, even for concurrent calls`() = runTest {
        val repository = repository()

        val first = async { repository.getCountries() }
        val second = async { repository.getCountries() }
        first.await()
        second.await()
        repository.getCountries()

        assertEquals(1, api.regionsCalls)
    }

    @Test
    fun `a failure is not cached`() = runTest {
        val repository = repository()
        val countries = api.regionsResponse
        api.regionsResponse = { throw IOException("offline") }

        assertEquals(AppError.Network, repository.getCountries().exceptionOrNull())

        api.regionsResponse = countries
        assertEquals(2, repository.getCountries().getOrThrow().size)
    }
}
