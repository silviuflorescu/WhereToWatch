package com.coroutine.mastery.wheretowatch.data.di

import com.coroutine.mastery.wheretowatch.data.preferences.RegionPreferencesDataStore
import com.coroutine.mastery.wheretowatch.data.repository.CountryRepositoryImpl
import com.coroutine.mastery.wheretowatch.data.repository.MovieRepositoryImpl
import com.coroutine.mastery.wheretowatch.domain.repository.CountryRepository
import com.coroutine.mastery.wheretowatch.domain.repository.MovieRepository
import com.coroutine.mastery.wheretowatch.domain.repository.RegionPreferences
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    @Binds
    abstract fun bindCountryRepository(impl: CountryRepositoryImpl): CountryRepository

    @Binds
    abstract fun bindRegionPreferences(impl: RegionPreferencesDataStore): RegionPreferences
}
