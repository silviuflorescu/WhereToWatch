package com.coroutine.mastery.wheretowatch.data.mapper

import com.coroutine.mastery.wheretowatch.domain.model.AppError
import java.io.IOException
import retrofit2.HttpException

fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    is IOException -> AppError.Network
    is HttpException -> if (code() == 401) AppError.Unauthorized else AppError.Server(code())
    else -> AppError.Unknown(this)
}

fun <T> Result<T>.mapFailureToAppError(): Result<T> =
    exceptionOrNull()?.let { Result.failure(it.toAppError()) } ?: this
