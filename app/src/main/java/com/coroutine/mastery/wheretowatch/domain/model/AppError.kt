package com.coroutine.mastery.wheretowatch.domain.model

sealed class AppError : Exception() {
    data object Network : AppError()

    data object Unauthorized : AppError()

    data class Server(val code: Int) : AppError()

    data class Unknown(override val cause: Throwable?) : AppError()
}
