package com.coroutine.mastery.wheretowatch.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.coroutine.mastery.wheretowatch.R
import com.coroutine.mastery.wheretowatch.domain.model.Provider
import com.coroutine.mastery.wheretowatch.domain.model.WatchProviders
import com.coroutine.mastery.wheretowatch.ui.search.ProvidersState
import com.coroutine.mastery.wheretowatch.ui.theme.WhereToWatchTheme

private val LabelHeight = 16.dp
private val LogoSize = 32.dp
private val LabelToLogosSpacing = 4.dp

private val ProvidersRowHeight = LabelHeight + LabelToLogosSpacing + LogoSize

@Composable
fun ProvidersRow(
    state: ProvidersState,
    regionCode: String?,
    regionName: String,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.heightIn(min = ProvidersRowHeight)) {
        when {
            state is ProvidersState.Failed -> HintText(stringResource(R.string.providers_failed))

            state is ProvidersState.Loading || regionCode == null -> ProvidersPlaceholder()

            state is ProvidersState.Loaded -> {
                val providers = state.forCountry(regionCode)
                when {
                    providers.stream.isNotEmpty() ->
                        ProviderLogos(stringResource(R.string.providers_stream), providers.stream)

                    !providers.isEmpty ->
                        ProviderLogos(
                            label = stringResource(R.string.providers_rent_buy),
                            providers = (providers.rent + providers.buy).distinctBy { it.id },
                        )

                    else -> HintText(stringResource(R.string.providers_none, regionName))
                }
            }
        }
    }
}

@Composable
private fun ProvidersPlaceholder() {
    val description = stringResource(R.string.providers_loading)
    Column(
        modifier = Modifier.semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(LabelToLogosSpacing),
    ) {
        Box(
            modifier = Modifier
                .size(width = 48.dp, height = LabelHeight)
                .clip(RoundedCornerShape(4.dp))
                .shimmer(),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) {
                Box(
                    modifier = Modifier
                        .size(LogoSize)
                        .clip(RoundedCornerShape(8.dp))
                        .shimmer(),
                )
            }
        }
    }
}

@Composable
private fun ProviderLogos(label: String, providers: List<Provider>, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(LabelToLogosSpacing)) {
        Text(
            text = label,
            modifier = Modifier.height(LabelHeight),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            providers.forEach { ProviderLogo(it) }
        }
    }
}

@Composable
private fun ProviderLogo(provider: Provider) {
    val shape = RoundedCornerShape(8.dp)
    if (provider.logoUrl != null) {
        AsyncImage(
            model = provider.logoUrl,
            contentDescription = provider.name,
            modifier = Modifier
                .size(LogoSize)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
    } else {
        Box(
            modifier = Modifier
                .size(LogoSize)
                .clip(shape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = provider.name.take(1),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun HintText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Preview(showBackground = true)
@Composable
private fun ProvidersRowPreview() {
    WhereToWatchTheme {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ProvidersRow(ProvidersState.Loading, regionCode = "RO", regionName = "Romania")
            ProvidersRow(
                ProvidersState.Loaded(
                    mapOf(
                        "RO" to WatchProviders(
                            stream = listOf(Provider(8, "Netflix", null), Provider(1899, "Max", null)),
                            rent = emptyList(),
                            buy = emptyList(),
                            link = null,
                        )
                    )
                ),
                regionCode = "RO",
                regionName = "Romania",
            )
            ProvidersRow(ProvidersState.Loaded(emptyMap()), regionCode = "RO", regionName = "Romania")
            ProvidersRow(ProvidersState.Failed, regionCode = "RO", regionName = "Romania")
        }
    }
}
