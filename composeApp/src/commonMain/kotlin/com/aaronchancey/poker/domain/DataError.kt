package com.aaronchancey.poker.domain

sealed interface DataError : Error {
    enum class Remote : DataError {
        BAD_REQUEST,
        REQUEST_TIMEOUT,
        TOO_MANY_REQUESTS,
        NO_INTERNET,
        SERVER,
        SERIALIZATION,
        UNKNOWN,
    }
}
