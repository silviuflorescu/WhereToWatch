package com.coroutine.mastery.wheretowatch.domain.usecase

import com.coroutine.mastery.wheretowatch.domain.model.MovieDetails
import com.coroutine.mastery.wheretowatch.domain.repository.MovieRepository
import javax.inject.Inject

class GetMovieDetailsUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(movieId: Int): Result<MovieDetails> =
        repository.getMovieDetails(movieId)
}
