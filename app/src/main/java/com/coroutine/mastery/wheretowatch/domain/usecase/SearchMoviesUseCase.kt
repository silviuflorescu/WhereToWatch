package com.coroutine.mastery.wheretowatch.domain.usecase

import com.coroutine.mastery.wheretowatch.domain.model.MoviePage
import com.coroutine.mastery.wheretowatch.domain.repository.MovieRepository
import javax.inject.Inject

class SearchMoviesUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(query: String, page: Int = 1): Result<MoviePage> =
        repository.searchMovies(query, page)
}
