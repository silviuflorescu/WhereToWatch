package com.coroutine.mastery.wheretowatch.domain.usecase

import com.coroutine.mastery.wheretowatch.domain.model.Trailer
import com.coroutine.mastery.wheretowatch.domain.repository.MovieRepository
import javax.inject.Inject

class GetTrailerUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(movieId: Int): Result<Trailer?> =
        repository.getTrailer(movieId)
}
