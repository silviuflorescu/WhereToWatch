package com.coroutine.mastery.wheretowatch.domain.repository

import com.coroutine.mastery.wheretowatch.domain.model.Country

interface CountryRepository {
    suspend fun getCountries(): Result<List<Country>>
}
