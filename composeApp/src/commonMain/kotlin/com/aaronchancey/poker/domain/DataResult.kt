package com.aaronchancey.poker.domain

sealed interface DataResult<out D, out E : Error> {
    data class Success<out D>(val data: D) : DataResult<D, Nothing>
    data class Error<out E : com.aaronchancey.poker.domain.Error>(val error: E, val data: String? = null) : DataResult<Nothing, E>
}

inline fun <T, E : Error, R> DataResult<T, E>.map(map: (T) -> R): DataResult<R, E> = when (this) {
    is DataResult.Error -> DataResult.Error(error, data)
    is DataResult.Success -> DataResult.Success(map(data))
}

fun <T, E : Error> DataResult<T, E>.asEmptyDataResult(): EmptyResult<E> = map { }

inline fun <T, E : Error> DataResult<T, E>.onSuccess(action: (T) -> Unit): DataResult<T, E> = when (this) {
    is DataResult.Error -> this

    is DataResult.Success -> {
        action(data)
        this
    }
}

inline fun <T, E : Error> DataResult<T, E>.onError(action: (E, String?) -> Unit): DataResult<T, E> = when (this) {
    is DataResult.Error -> {
        action(error, data)
        this
    }

    is DataResult.Success -> this
}

typealias EmptyResult<E> = DataResult<Unit, E>
