package com.coroutine.mastery.wheretowatch.ui.details

import com.coroutine.mastery.wheretowatch.domain.model.CastMember
import com.coroutine.mastery.wheretowatch.domain.model.Movie
import com.coroutine.mastery.wheretowatch.domain.model.MovieDetails
import com.coroutine.mastery.wheretowatch.domain.model.Trailer
import com.coroutine.mastery.wheretowatch.ui.search.ProvidersState

sealed interface Section<out T> {
    data object Loading : Section<Nothing>
    data class Loaded<T>(val data: T) : Section<T>
    data class Failed(val error: Throwable) : Section<Nothing>
}

data class MovieDetailsUiState(
    val details: Section<MovieDetails> = Section.Loading,
    val cast: Section<List<CastMember>> = Section.Loading,
    val trailer: Section<Trailer?> = Section.Loading,
    val recommendations: Section<List<Movie>> = Section.Loading,
    val providers: ProvidersState = ProvidersState.Loading,
)
