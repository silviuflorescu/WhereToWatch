package com.coroutine.mastery.wheretowatch.domain.usecase

import com.coroutine.mastery.wheretowatch.domain.model.Movie
import com.coroutine.mastery.wheretowatch.domain.repository.MovieRepository
import javax.inject.Inject

class GetRecommendationsUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(movieId: Int): Result<List<Movie>> =
        repository.getRecommendations(movieId)
}
