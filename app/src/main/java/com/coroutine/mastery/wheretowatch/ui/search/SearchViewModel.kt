package com.coroutine.mastery.wheretowatch.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coroutine.mastery.wheretowatch.domain.model.Movie
import com.coroutine.mastery.wheretowatch.domain.usecase.GetWatchProvidersUseCase
import com.coroutine.mastery.wheretowatch.domain.usecase.SearchMoviesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchMovies: SearchMoviesUseCase,
    private val getWatchProviders: GetWatchProvidersUseCase,
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")

    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val retryTrigger = MutableSharedFlow<Unit>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    ).apply { tryEmit(Unit) }

    private val nextPageRequests = Channel<Unit>(Channel.RENDEZVOUS)

    val uiState: StateFlow<SearchUiState> =
        combine(
            _searchQuery
                .debounce { if (it.isBlank()) 0L else DEBOUNCE_MILLIS }
                .map { it.trim() }
                .distinctUntilChanged(),
            retryTrigger,
        ) { query, _ -> query }
            .flatMapLatest { query ->
                if (query.length < MIN_QUERY_LENGTH) flowOf(SearchEvent.Cleared) else search(query)
            }
            .scan<SearchEvent, SearchUiState>(SearchUiState.Idle, ::reduce)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Lazily,
                initialValue = SearchUiState.Idle,
            )

    fun onQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onClearQuery() {
        _searchQuery.value = ""
    }

    fun onRetry() {
        retryTrigger.tryEmit(Unit)
    }

    fun onLoadNextPage() {
        nextPageRequests.trySend(Unit)
    }

    private fun search(query: String): Flow<SearchEvent> = channelFlow {
        send(SearchEvent.Started)

        val firstPage = searchMovies(query, page = 1).getOrElse { error ->
            send(SearchEvent.Failed(error))
            return@channelFlow
        }
        send(SearchEvent.MoviesLoaded(query, firstPage.movies, firstPage.hasMore))

        val semaphore = Semaphore(MAX_CONCURRENT_PROVIDER_REQUESTS)
        loadProviders(firstPage.movies, semaphore)

        var page = firstPage
        while (page.hasMore) {
            nextPageRequests.receive()
            send(SearchEvent.NextPageLoading)
            searchMovies(query, page = page.page + 1)
                .onSuccess { next ->
                    page = next
                    send(SearchEvent.NextPageLoaded(next.movies, next.hasMore))
                    loadProviders(next.movies, semaphore)
                }
                .onFailure { error -> send(SearchEvent.NextPageFailed(error)) }
        }
    }

    private fun ProducerScope<SearchEvent>.loadProviders(movies: List<Movie>, semaphore: Semaphore) {
        movies.forEach { movie ->
            launch {
                val result = semaphore.withPermit { getWatchProviders(movie.id) }
                send(SearchEvent.ProvidersLoaded(movie.id, result))
            }
        }
    }

    companion object {
        const val DEBOUNCE_MILLIS = 400L
        const val MIN_QUERY_LENGTH = 2
        const val MAX_CONCURRENT_PROVIDER_REQUESTS = 4
    }
}
