package com.coroutine.mastery.wheretowatch.domain.model

data class MoviePage(
    val movies: List<Movie>,
    val page: Int,
    val totalPages: Int,
) {
    val hasMore: Boolean get() = page < totalPages
}
