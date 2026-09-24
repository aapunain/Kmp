package com.self.kmp.domain.common

/**
 * The result type crossing the repository boundary.
 *
 * Because the failure case carries an [AppError], every caller is forced by the
 * type system to deal with failure. Nothing above the data layer can throw a
 * transport exception at us by surprise.
 */
sealed interface AppResult<out T> {

    data class Success<out T>(val data: T) : AppResult<T>

    data class Failure(val error: AppError) : AppResult<Nothing>
}

inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

inline fun <T, R> AppResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (AppError) -> R,
): R = when (this) {
    is AppResult.Success -> onSuccess(data)
    is AppResult.Failure -> onFailure(error)
}

fun <T> T.asSuccess(): AppResult<T> = AppResult.Success(this)

fun AppError.asFailure(): AppResult<Nothing> = AppResult.Failure(this)
