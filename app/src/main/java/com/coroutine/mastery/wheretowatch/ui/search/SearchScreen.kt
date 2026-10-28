package com.coroutine.mastery.wheretowatch.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coroutine.mastery.wheretowatch.R
import com.coroutine.mastery.wheretowatch.domain.model.Movie
import com.coroutine.mastery.wheretowatch.domain.model.RegionSource
import com.coroutine.mastery.wheretowatch.domain.model.ResolvedRegion
import com.coroutine.mastery.wheretowatch.ui.components.EmptyView
import com.coroutine.mastery.wheretowatch.ui.components.ErrorView
import com.coroutine.mastery.wheretowatch.ui.components.IdleView
import com.coroutine.mastery.wheretowatch.ui.components.LoadingView
import com.coroutine.mastery.wheretowatch.ui.components.MovieCard
import com.coroutine.mastery.wheretowatch.ui.region.RegionPicker
import com.coroutine.mastery.wheretowatch.ui.region.RegionUiState
import com.coroutine.mastery.wheretowatch.ui.region.RegionViewModel
import com.coroutine.mastery.wheretowatch.ui.theme.WhereToWatchTheme
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull

@Composable
fun SearchScreen(
    regionViewModel: RegionViewModel,
    onMovieClick: (movieId: Int) -> Unit,
    viewModel: SearchViewModel = hiltViewModel(),
) {
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val regionUiState by regionViewModel.regionUiState.collectAsStateWithLifecycle()

    SearchContent(
        query = query,
        uiState = uiState,
        regionUiState = regionUiState,
        onQueryChange = viewModel::onQueryChange,
        onClearQuery = viewModel::onClearQuery,
        onRetry = viewModel::onRetry,
        onLoadNextPage = viewModel::onLoadNextPage,
        onMovieClick = onMovieClick,
        regionPicker = { RegionPicker(regionViewModel) },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchContent(
    query: String,
    uiState: SearchUiState,
    regionUiState: RegionUiState,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onRetry: () -> Unit,
    onLoadNextPage: () -> Unit = {},
    onMovieClick: (movieId: Int) -> Unit = {},
    regionPicker: @Composable () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                actions = { regionPicker() },
            )
        },
        bottomBar = { Attribution() },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                onClearQuery = onClearQuery,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
            when (uiState) {
                SearchUiState.Idle -> IdleView()
                SearchUiState.Loading -> LoadingView()
                is SearchUiState.Empty -> EmptyView(query = uiState.query)
                is SearchUiState.Error -> ErrorView(error = uiState.error, onRetry = onRetry)
                is SearchUiState.Results -> {
                    val regionCode = regionUiState.resolved?.code
                    MovieList(
                        state = uiState,
                        regionCode = regionCode,
                        regionName = regionCode?.let(regionUiState::countryName).orEmpty(),
                        onMovieClick = onMovieClick,
                        onLoadNextPage = onLoadNextPage,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text(stringResource(R.string.search_hint)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClearQuery) {
                    Icon(Icons.Filled.Clear, contentDescription = stringResource(R.string.clear_search))
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboardController?.hide() }),
    )
}

@Composable
private fun MovieList(
    state: SearchUiState.Results,
    regionCode: String?,
    regionName: String,
    onMovieClick: (Int) -> Unit,
    onLoadNextPage: () -> Unit,
) {
    val listState = rememberLazyListState()
    val nextPage by rememberUpdatedState(state.nextPage)
    val loadNextPage by rememberUpdatedState(onLoadNextPage)

    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val nearEnd = lastVisible >= layoutInfo.totalItemsCount - NEXT_PAGE_THRESHOLD
            layoutInfo.totalItemsCount.takeIf { nearEnd && nextPage == NextPageState.Idle }
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { loadNextPage() }
    }

    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = state.movies, key = { it.id }) { movie ->
            MovieCard(
                movie = movie,
                providersState = state.providers[movie.id] ?: ProvidersState.Loading,
                regionCode = regionCode,
                regionName = regionName,
                onClick = { onMovieClick(movie.id) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
        when (val next = state.nextPage) {
            NextPageState.Loading -> item(key = "next-page") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                }
            }
            is NextPageState.Failed -> item(key = "next-page") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.next_page_failed),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onLoadNextPage) { Text(stringResource(R.string.retry)) }
                }
            }
            NextPageState.Idle, NextPageState.End -> Unit
        }
    }
}

private const val NEXT_PAGE_THRESHOLD = 5

@Composable
private fun Attribution() {
    Text(
        text = stringResource(R.string.attribution),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(8.dp),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchContentPreview() {
    val movie = Movie(
        id = 27205,
        title = "Inception",
        year = 2010,
        genres = listOf("Action", "Science Fiction"),
        overview = "A thief who steals corporate secrets through dream-sharing technology.",
        posterUrl = null,
        rating = 8.4,
        voteCount = 37000,
    )
    WhereToWatchTheme {
        SearchContent(
            query = "inception",
            uiState = SearchUiState.Results(
                movies = listOf(movie),
                providers = mapOf(movie.id to ProvidersState.Loading),
            ),
            regionUiState = RegionUiState(resolved = ResolvedRegion("US", RegionSource.Location)),
            onQueryChange = {},
            onClearQuery = {},
            onRetry = {},
        )
    }
}
