package com.coroutine.mastery.wheretowatch.data.repository

import com.coroutine.mastery.wheretowatch.core.di.IoDispatcher
import com.coroutine.mastery.wheretowatch.core.util.suspendRunCatching
import com.coroutine.mastery.wheretowatch.data.mapper.mapFailureToAppError
import com.coroutine.mastery.wheretowatch.data.remote.TmdbApi
import com.coroutine.mastery.wheretowatch.data.remote.TmdbConfig
import com.coroutine.mastery.wheretowatch.domain.model.Country
import com.coroutine.mastery.wheretowatch.domain.repository.CountryRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@Singleton
class CountryRepositoryImpl @Inject constructor(
    private val api: TmdbApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : CountryRepository {

    private val countriesMutex = Mutex()
    private var countriesCache: List<Country>? = null

    override suspend fun getCountries(): Result<List<Country>> =
        withContext(ioDispatcher) {
            countriesMutex.withLock {
                countriesCache?.let { Result.success(it) } ?: suspendRunCatching {
                    api.getWatchProviderRegions(TmdbConfig.LANGUAGE).results
                        .map { Country(code = it.code, name = it.englishName) }
                        .sortedBy { it.name }
                }.onSuccess { countriesCache = it }
            }.mapFailureToAppError()
        }
}
