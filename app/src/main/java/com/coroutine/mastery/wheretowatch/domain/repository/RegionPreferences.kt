package com.coroutine.mastery.wheretowatch.domain.repository

import com.coroutine.mastery.wheretowatch.domain.model.RegionSelection
import kotlinx.coroutines.flow.Flow

interface RegionPreferences {
    val selection: Flow<RegionSelection>

    suspend fun setSelection(selection: RegionSelection)

    val locationPermissionAsked: Flow<Boolean>

    suspend fun setLocationPermissionAsked()
}
