package com.coroutine.mastery.wheretowatch.ui.region

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coroutine.mastery.wheretowatch.domain.model.RegionSelection

@Composable
fun RegionPicker(regionViewModel: RegionViewModel, modifier: Modifier = Modifier) {
    val regionUiState by regionViewModel.regionUiState.collectAsStateWithLifecycle()
    val shouldAskLocationPermission by regionViewModel.shouldAskLocationPermission.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        regionViewModel.onLocationPermissionResult()
    }

    LaunchedEffect(shouldAskLocationPermission) {
        if (shouldAskLocationPermission) {
            regionViewModel.onLocationPermissionRequested()
            if (!context.hasLocationPermission()) {
                permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        }
    }

    RegionSelector(
        state = regionUiState,
        onMenuOpened = regionViewModel::onCountryMenuOpened,
        onRegionSelected = { selection ->
            regionViewModel.onRegionSelected(selection)
            if (selection == RegionSelection.Auto && !context.hasLocationPermission()) {
                permissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }
        },
        modifier = modifier,
    )
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED
