package com.aaronchancey.poker.network

import com.aaronchancey.poker.domain.DataError
import com.aaronchancey.poker.domain.DataResult
import io.ktor.client.call.NoTransformationFoundException
import io.ktor.client.call.body
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.statement.HttpResponse
import io.ktor.util.network.UnresolvedAddressException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

suspend inline fun <reified T> safeCall(
    execute: () -> HttpResponse,
): DataResult<T, DataError.Remote> {
    val response = try {
        execute()
    } catch (e: SocketTimeoutException) {
        println(e.toString())
        return DataResult.Error(DataError.Remote.REQUEST_TIMEOUT)
    } catch (e: UnresolvedAddressException) {
        println(e.toString())
        return DataResult.Error(DataError.Remote.NO_INTERNET)
    } catch (e: Exception) {
        currentCoroutineContext().ensureActive()
        println(e.toString())
        return DataResult.Error(DataError.Remote.UNKNOWN)
    }

    return responseToResult(response)
}

suspend inline fun <reified T> responseToResult(
    response: HttpResponse,
): DataResult<T, DataError.Remote> {
    println("Response: $response")
    return when (response.status.value) {
        in 200..299 -> {
            try {
                DataResult.Success(response.body<T>())
            } catch (e: NoTransformationFoundException) {
                println(e)
                DataResult.Error(DataError.Remote.SERIALIZATION)
            }
        }

        400 -> DataResult.Error(DataError.Remote.BAD_REQUEST, response.body())

        408 -> DataResult.Error(DataError.Remote.REQUEST_TIMEOUT, response.body())

        429 -> DataResult.Error(DataError.Remote.TOO_MANY_REQUESTS, response.body())

        in 500..599 -> DataResult.Error(DataError.Remote.SERVER, response.body())

        else -> DataResult.Error(DataError.Remote.UNKNOWN, response.body())
    }
}
