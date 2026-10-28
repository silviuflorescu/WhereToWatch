package com.coroutine.mastery.wheretowatch.data.preferences

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.coroutine.mastery.wheretowatch.domain.model.RegionSelection
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class RegionPreferencesDataStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun TestScope.preferences() = RegionPreferencesDataStore(
        PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { File(tempFolder.root, "test.preferences_pb") },
        )
    )

    @Test
    fun `defaults to Auto`() = runTest {
        assertEquals(RegionSelection.Auto, preferences().selection.first())
    }

    @Test
    fun `saves a manual country and switches back to Auto`() = runTest {
        val preferences = preferences()

        preferences.setSelection(RegionSelection.Manual("RO"))
        assertEquals(RegionSelection.Manual("RO"), preferences.selection.first())

        preferences.setSelection(RegionSelection.Auto)
        assertEquals(RegionSelection.Auto, preferences.selection.first())
    }

    @Test
    fun `remembers that the location permission was asked`() = runTest {
        val preferences = preferences()
        assertEquals(false, preferences.locationPermissionAsked.first())

        preferences.setLocationPermissionAsked()

        assertEquals(true, preferences.locationPermissionAsked.first())
    }
}
