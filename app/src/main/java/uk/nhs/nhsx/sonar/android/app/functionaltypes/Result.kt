package uk.nhs.nhsx.sonar.android.app.functionaltypes

sealed class Result<T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure<T>(val reason: Explanation) : Result<T>() {
        constructor(exception: Throwable) : this(Explanation(exception))
    }

    fun <U> map(function: (T) -> U): Result<U> =
        when (this) {
            is Success -> Success(function(value))
            is Failure -> Failure(reason)
        }

    suspend fun <U> coMap(function: suspend (T) -> U): Result<U> =
        when (this) {
            is Success -> Success(function(value))
            is Failure -> Failure(reason)
        }

    fun <U> bind(function: (T) -> Result<U>): Result<U> =
        when (this) {
            is Success -> function(value)
            is Failure -> Failure(reason)
        }

    suspend fun <U> coBind(function: suspend (T) -> Result<U>): Result<U> =
        when (this) {
            is Success -> function(value)
            is Failure -> Failure(reason)
        }

    fun orElse(function: (Explanation) -> T): T =
        when (this) {
            is Success -> value
            is Failure -> function(reason)
        }

    @Throws
    fun orThrow(): T =
        orElse { throw it.exception() }
}

data class Explanation(val message: String, val exception: Throwable? = null) {
    constructor(exception: Throwable) : this(exception.message ?: "An exception occurred", exception)

    fun exception(): Throwable =
        exception ?: Exception(message)
}

suspend fun <T> runSafely(function: suspend () -> T): Result<T> =
    runCatching { Result.Success(function()) }.getOrElse { Result.Failure(it) }
