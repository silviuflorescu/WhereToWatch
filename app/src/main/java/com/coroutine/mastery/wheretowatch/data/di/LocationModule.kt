package com.coroutine.mastery.wheretowatch.data.di

import com.coroutine.mastery.wheretowatch.data.location.AndroidCountryDetector
import com.coroutine.mastery.wheretowatch.data.location.AndroidDeviceLocaleSource
import com.coroutine.mastery.wheretowatch.data.location.AndroidGeocoderSource
import com.coroutine.mastery.wheretowatch.data.location.AndroidLocationSource
import com.coroutine.mastery.wheretowatch.data.location.AndroidNetworkCountrySource
import com.coroutine.mastery.wheretowatch.data.location.DeviceLocaleSource
import com.coroutine.mastery.wheretowatch.data.location.GeocoderSource
import com.coroutine.mastery.wheretowatch.data.location.LocationSource
import com.coroutine.mastery.wheretowatch.data.location.NetworkCountrySource
import com.coroutine.mastery.wheretowatch.domain.repository.CountryDetector
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class LocationModule {

    @Binds
    abstract fun bindCountryDetector(impl: AndroidCountryDetector): CountryDetector

    @Binds
    abstract fun bindLocationSource(impl: AndroidLocationSource): LocationSource

    @Binds
    abstract fun bindGeocoderSource(impl: AndroidGeocoderSource): GeocoderSource

    @Binds
    abstract fun bindNetworkCountrySource(impl: AndroidNetworkCountrySource): NetworkCountrySource

    @Binds
    abstract fun bindDeviceLocaleSource(impl: AndroidDeviceLocaleSource): DeviceLocaleSource
}
