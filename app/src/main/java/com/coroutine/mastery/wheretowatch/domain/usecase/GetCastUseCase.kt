package com.coroutine.mastery.wheretowatch.domain.usecase

import com.coroutine.mastery.wheretowatch.domain.model.CastMember
import com.coroutine.mastery.wheretowatch.domain.repository.MovieRepository
import javax.inject.Inject

class GetCastUseCase @Inject constructor(
    private val repository: MovieRepository,
) {
    suspend operator fun invoke(movieId: Int): Result<List<CastMember>> =
        repository.getCast(movieId)
}
