package com.coroutine.mastery.wheretowatch.ui.details

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.SubcomposeAsyncImage
import com.coroutine.mastery.wheretowatch.R
import com.coroutine.mastery.wheretowatch.domain.model.CastMember
import com.coroutine.mastery.wheretowatch.domain.model.Movie
import com.coroutine.mastery.wheretowatch.domain.model.MovieDetails
import com.coroutine.mastery.wheretowatch.domain.model.Provider
import com.coroutine.mastery.wheretowatch.domain.model.RegionSource
import com.coroutine.mastery.wheretowatch.domain.model.ResolvedRegion
import com.coroutine.mastery.wheretowatch.domain.model.Trailer
import com.coroutine.mastery.wheretowatch.domain.model.WatchProviders
import com.coroutine.mastery.wheretowatch.ui.components.ErrorView
import com.coroutine.mastery.wheretowatch.ui.components.GenreChip
import com.coroutine.mastery.wheretowatch.ui.components.LoadingView
import com.coroutine.mastery.wheretowatch.ui.components.shimmer
import com.coroutine.mastery.wheretowatch.ui.region.RegionPicker
import com.coroutine.mastery.wheretowatch.ui.region.RegionUiState
import com.coroutine.mastery.wheretowatch.ui.region.RegionViewModel
import com.coroutine.mastery.wheretowatch.ui.search.ProvidersState
import com.coroutine.mastery.wheretowatch.ui.theme.WhereToWatchTheme

@Composable
fun MovieDetailsScreen(
    regionViewModel: RegionViewModel,
    onBack: () -> Unit,
    onMovieClick: (movieId: Int) -> Unit,
    viewModel: MovieDetailsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val regionUiState by regionViewModel.regionUiState.collectAsStateWithLifecycle()
    MovieDetailsContent(
        uiState = uiState,
        regionUiState = regionUiState,
        onBack = onBack,
        onRetry = viewModel::onRetry,
        onMovieClick = onMovieClick,
        regionPicker = { RegionPicker(regionViewModel) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailsContent(
    uiState: MovieDetailsUiState,
    regionUiState: RegionUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onMovieClick: (movieId: Int) -> Unit,
    regionPicker: @Composable () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = (uiState.details as? Section.Loaded)?.data?.title.orEmpty()
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = { regionPicker() },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { innerPadding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        when (val details = uiState.details) {
            Section.Loading -> LoadingView(modifier)
            is Section.Failed -> ErrorView(error = details.error, onRetry = onRetry, modifier = modifier)
            is Section.Loaded -> DetailsList(details.data, uiState, regionUiState, onMovieClick, modifier)
        }
    }
}

@Composable
private fun DetailsList(
    details: MovieDetails,
    uiState: MovieDetailsUiState,
    regionUiState: RegionUiState,
    onMovieClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        details.backdropUrl?.let { url -> item(key = "backdrop") { Backdrop(url) } }
        item(key = "header") { Header(details, uiState.trailer) }
        if (details.overview.isNotBlank()) {
            item(key = "overview") {
                SectionColumn(title = stringResource(R.string.details_overview)) {
                    Text(
                        text = details.overview,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
        item(key = "providers") { WhereToWatch(uiState.providers, regionUiState) }
        castSection(uiState.cast)
        recommendationsSection(uiState.recommendations, onMovieClick)
        item(key = "bottom") { Spacer(Modifier.navigationBarsPadding()) }
    }
}

@Composable
private fun Backdrop(url: String) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    )
}

@Composable
private fun Header(details: MovieDetails, trailer: Section<Trailer?>) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Poster(url = details.posterUrl, title = details.title, width = 110.dp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = details.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                details.tagline?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val meta = listOfNotNull(details.year?.toString(), details.runtimeMinutes?.let(::formatRuntime))
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta.joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (details.voteCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Star,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            text = stringResource(R.string.rating_votes, details.rating, details.voteCount),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
        if (details.genres.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                details.genres.forEach { GenreChip(it) }
            }
        }
        val trailerData = (trailer as? Section.Loaded)?.data
        if (trailerData != null) {
            val uriHandler = LocalUriHandler.current
            Button(onClick = { uriHandler.openUri(trailerData.url) }) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Text(stringResource(R.string.details_watch_trailer), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun WhereToWatch(providersState: ProvidersState, regionUiState: RegionUiState) {
    val regionCode = regionUiState.resolved?.code
    val regionName = regionCode?.let(regionUiState::countryName)
    val title = regionName?.let { stringResource(R.string.details_where_to_watch, it) }
        ?: stringResource(R.string.details_where_to_watch_detecting)
    SectionColumn(title = title) {
        when {
            providersState is ProvidersState.Failed -> Hint(stringResource(R.string.providers_failed))
            providersState is ProvidersState.Loading || regionCode == null ->
                ShimmerRow(count = 4, size = 48.dp, shape = RoundedCornerShape(12.dp))
            providersState is ProvidersState.Loaded -> {
                val providers = providersState.forCountry(regionCode)
                ProviderGroups(providers, regionName = regionName.orEmpty())
            }
        }
    }
}

@Composable
private fun ProviderGroups(providers: WatchProviders, regionName: String) {
    if (providers.isEmpty) {
        Hint(stringResource(R.string.providers_none, regionName))
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(
            R.string.providers_stream to providers.stream,
            R.string.providers_rent to providers.rent,
            R.string.providers_buy to providers.buy,
        ).filter { (_, list) -> list.isNotEmpty() }.forEach { (label, list) ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = stringResource(label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(list, key = { it.id }) { ProviderTile(it) }
                }
            }
        }
    }
}

@Composable
private fun ProviderTile(provider: Provider) {
    Column(
        modifier = Modifier.width(64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AsyncImage(
            model = provider.logoUrl,
            contentDescription = null,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Text(
            text = provider.name,
            style = MaterialTheme.typography.labelSmall,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun LazyListScope.castSection(cast: Section<List<CastMember>>) {
    when (cast) {
        Section.Loading -> item(key = "cast") {
            SectionColumn(title = stringResource(R.string.details_cast)) {
                ShimmerRow(count = 5, size = 72.dp, shape = CircleShape)
            }
        }
        is Section.Loaded -> if (cast.data.isNotEmpty()) {
            item(key = "cast") {
                SectionColumn(title = stringResource(R.string.details_cast)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(cast.data, key = { it.id }) { CastItem(it) }
                    }
                }
            }
        }
        is Section.Failed -> Unit
    }
}

@Composable
private fun CastItem(member: CastMember) {
    Column(
        modifier = Modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SubcomposeAsyncImage(
            model = member.photoUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            error = { PlaceholderIcon(Icons.Outlined.Person) },
        )
        Text(
            text = member.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = member.character.orEmpty(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun LazyListScope.recommendationsSection(
    recommendations: Section<List<Movie>>,
    onMovieClick: (Int) -> Unit,
) {
    when (recommendations) {
        Section.Loading -> item(key = "recommendations") {
            SectionColumn(title = stringResource(R.string.details_recommended)) {
                ShimmerRow(count = 4, size = 110.dp, height = 165.dp, shape = RoundedCornerShape(10.dp))
            }
        }
        is Section.Loaded -> if (recommendations.data.isNotEmpty()) {
            item(key = "recommendations") {
                SectionColumn(title = stringResource(R.string.details_recommended)) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(recommendations.data, key = { it.id }) { movie ->
                            Column(
                                modifier = Modifier
                                    .width(110.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onMovieClick(movie.id) },
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Poster(url = movie.posterUrl, title = movie.title, width = 110.dp)
                                Text(
                                    text = movie.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    minLines = 2,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
        is Section.Failed -> Unit
    }
}

@Composable
private fun SectionColumn(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        content()
    }
}

@Composable
private fun Poster(url: String?, title: String, width: Dp) {
    SubcomposeAsyncImage(
        model = url,
        contentDescription = title,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .width(width)
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        loading = { PlaceholderIcon(Icons.Outlined.Movie) },
        error = { PlaceholderIcon(Icons.Outlined.Movie) },
    )
}

@Composable
private fun PlaceholderIcon(icon: ImageVector) {
    Box(contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ShimmerRow(count: Int, size: Dp, shape: Shape, height: Dp = size) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        repeat(count) {
            Box(
                Modifier
                    .width(size)
                    .height(height)
                    .clip(shape)
                    .shimmer()
            )
        }
    }
}

@Composable
private fun Hint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

private fun formatRuntime(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return if (hours > 0) "${hours}h ${rest}m" else "${rest}m"
}

@Preview(showBackground = true)
@Composable
private fun MovieDetailsPreview() {
    WhereToWatchTheme {
        MovieDetailsContent(
            uiState = MovieDetailsUiState(
                details = Section.Loaded(
                    MovieDetails(
                        id = 27205,
                        title = "Inception",
                        tagline = "Your mind is the scene of the crime.",
                        overview = "Cobb, a skilled thief who commits corporate espionage by infiltrating " +
                            "the subconscious of his targets, is offered a chance to regain his old life.",
                        year = 2010,
                        runtimeMinutes = 148,
                        genres = listOf("Action", "Science Fiction", "Adventure"),
                        rating = 8.4,
                        voteCount = 37000,
                        posterUrl = null,
                        backdropUrl = null,
                    )
                ),
                cast = Section.Loaded(listOf(CastMember(1, "Leonardo DiCaprio", "Cobb", null))),
                trailer = Section.Loaded(Trailer("Trailer", "https://www.youtube.com")),
                recommendations = Section.Loading,
                providers = ProvidersState.Loaded(
                    mapOf("US" to WatchProviders(listOf(Provider(8, "Netflix", null)), emptyList(), emptyList(), null))
                ),
            ),
            regionUiState = RegionUiState(resolved = ResolvedRegion("US", RegionSource.Location)),
            onBack = {},
            onRetry = {},
            onMovieClick = {},
        )
    }
}
