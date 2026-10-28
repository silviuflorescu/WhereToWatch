package com.coroutine.mastery.wheretowatch.ui.region

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.MyLocation
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coroutine.mastery.wheretowatch.R
import com.coroutine.mastery.wheretowatch.domain.model.Country
import com.coroutine.mastery.wheretowatch.domain.model.RegionSelection
import com.coroutine.mastery.wheretowatch.domain.model.RegionSource
import com.coroutine.mastery.wheretowatch.domain.model.ResolvedRegion
import com.coroutine.mastery.wheretowatch.ui.theme.WhereToWatchTheme

@Composable
fun RegionSelector(
    state: RegionUiState,
    onMenuOpened: () -> Unit,
    onRegionSelected: (RegionSelection) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("") }

    fun close() {
        expanded = false
        filter = ""
    }

    Box(modifier = modifier.padding(end = 8.dp)) {
        val code = state.resolved?.code
        Surface(
            onClick = {
                expanded = true
                onMenuOpened()
            },
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (code == null) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Text(text = "${flagEmoji(code)} $code", style = MaterialTheme.typography.labelLarge)
                }
                if (state.selection == RegionSelection.Auto) {
                    Text(
                        text = " · " + stringResource(R.string.region_auto),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = stringResource(R.string.region_selector_description),
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = ::close,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            modifier = Modifier.width(MENU_WIDTH),
        ) {
            OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                placeholder = { Text(stringResource(R.string.region_search_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
            )

            DropdownMenuItem(
                text = {
                    Column {
                        Text(stringResource(R.string.region_auto))
                        Text(
                            text = autoSubtitle(state),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                leadingIcon = { Icon(Icons.Outlined.MyLocation, contentDescription = null) },
                trailingIcon = { SelectedMark(state.selection == RegionSelection.Auto) },
                onClick = {
                    onRegionSelected(RegionSelection.Auto)
                    close()
                },
            )
            HorizontalDivider()

            if (state.countries.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.region_loading)) },
                    onClick = {},
                    enabled = false,
                )
            }
            val filteredCountries = remember(state.countries, filter) {
                state.countries.filter { filter.isBlank() || it.name.contains(filter.trim(), ignoreCase = true) }
            }
            val listHeight = (MENU_ITEM_HEIGHT * filteredCountries.size).coerceAtMost(MENU_LIST_MAX_HEIGHT)
            LazyColumn(modifier = Modifier.size(width = MENU_WIDTH, height = listHeight)) {
                items(items = filteredCountries, key = { it.code }) { country ->
                    DropdownMenuItem(
                        text = { Text("${flagEmoji(country.code)}  ${country.name}") },
                        trailingIcon = { SelectedMark(state.selection == RegionSelection.Manual(country.code)) },
                        onClick = {
                            onRegionSelected(RegionSelection.Manual(country.code))
                            close()
                        },
                    )
                }
            }
        }
    }
}

private val MENU_WIDTH = 300.dp
private val MENU_ITEM_HEIGHT = 48.dp
private val MENU_LIST_MAX_HEIGHT = 320.dp

@Composable
private fun autoSubtitle(state: RegionUiState): String {
    val resolved = state.resolved
    return when {
        state.selection != RegionSelection.Auto -> stringResource(R.string.region_auto_hint)
        resolved == null -> stringResource(R.string.region_detecting)
        else -> {
            val name = state.countryName(resolved.code)
            when (resolved.source) {
                RegionSource.Location -> stringResource(R.string.region_source_location, name)
                RegionSource.Network -> stringResource(R.string.region_source_network, name)
                RegionSource.DeviceLocale -> stringResource(R.string.region_source_locale, name)
                RegionSource.Default, RegionSource.Manual -> stringResource(R.string.region_source_default, name)
            }
        }
    }
}

@Composable
private fun SelectedMark(selected: Boolean) {
    if (selected) Icon(Icons.Filled.Check, contentDescription = null)
}

@Preview(showBackground = true)
@Composable
private fun RegionSelectorPreview() {
    WhereToWatchTheme {
        RegionSelector(
            state = RegionUiState(
                selection = RegionSelection.Auto,
                resolved = ResolvedRegion("RO", RegionSource.Location),
                countries = listOf(Country("DE", "Germany"), Country("RO", "Romania")),
            ),
            onMenuOpened = {},
            onRegionSelected = {},
        )
    }
}
