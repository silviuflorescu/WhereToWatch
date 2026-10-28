package com.coroutine.mastery.wheretowatch.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.coroutine.mastery.wheretowatch.domain.model.RegionSelection
import com.coroutine.mastery.wheretowatch.domain.repository.RegionPreferences
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class RegionPreferencesDataStore @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : RegionPreferences {

    private val preferences: Flow<Preferences> = dataStore.data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }

    override val selection: Flow<RegionSelection> = preferences
        .map { prefs ->
            when (val value = prefs[REGION_KEY]) {
                null, AUTO -> RegionSelection.Auto
                else -> RegionSelection.Manual(value)
            }
        }
        .distinctUntilChanged()

    override suspend fun setSelection(selection: RegionSelection) {
        dataStore.edit { prefs ->
            prefs[REGION_KEY] = when (selection) {
                RegionSelection.Auto -> AUTO
                is RegionSelection.Manual -> selection.code
            }
        }
    }

    override val locationPermissionAsked: Flow<Boolean> = preferences
        .map { it[LOCATION_PERMISSION_ASKED_KEY] ?: false }
        .distinctUntilChanged()

    override suspend fun setLocationPermissionAsked() {
        dataStore.edit { it[LOCATION_PERMISSION_ASKED_KEY] = true }
    }

    private companion object {
        const val AUTO = "AUTO"
        val REGION_KEY = stringPreferencesKey("region")
        val LOCATION_PERMISSION_ASKED_KEY = booleanPreferencesKey("location_permission_asked")
    }
}
