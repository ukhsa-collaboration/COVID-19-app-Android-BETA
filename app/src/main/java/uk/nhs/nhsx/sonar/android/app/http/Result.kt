package uk.nhs.nhsx.sonar.android.app.http

sealed class Result<T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure<T>(val reason: Explanation) : Result<T>() {
        constructor(exception: Exception) : this(Explanation(exception))
    }

    fun <U> map(function: (T) -> U): Result<U> =
        when (this) {
            is Success -> Success(function(value))
            is Failure -> Failure(reason)
        }

    fun orElse(function: (Explanation) -> T): T =
        when (this) {
            is Success -> value
            is Failure -> function(reason)
        }
}

data class Explanation(val message: String, val exception: Exception? = null) {
    constructor(exception: Exception) : this(exception.message ?: "An exception occurred", exception)
}
