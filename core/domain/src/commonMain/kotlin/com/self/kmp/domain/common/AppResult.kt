package com.self.kmp.domain.common

/**
 * The result type crossing the repository boundary.
 *
 * Because the failure case carries an [AppError], every caller is forced by the
 * type system to deal with failure. Nothing above the data layer can throw a
 * transport exception at us by surprise.
 */
public sealed interface AppResult<out T> {
    public data class Success<out T>(
        val data: T,
    ) : AppResult<T>

    public data class Failure(
        val error: AppError,
    ) : AppResult<Nothing>
}

public inline fun <T, R> AppResult<T>.map(transform: (T) -> R): AppResult<R> = when (this) {
    is AppResult.Success -> AppResult.Success(transform(data))
    is AppResult.Failure -> this
}

public inline fun <T, R> AppResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (AppError) -> R,
): R = when (this) {
    is AppResult.Success -> onSuccess(data)
    is AppResult.Failure -> onFailure(error)
}

public fun <T> T.asSuccess(): AppResult<T> = AppResult.Success(this)

public fun AppError.asFailure(): AppResult<Nothing> = AppResult.Failure(this)
