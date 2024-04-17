package com.poker.common.data.remote

import com.poker.common.core.Resource
import com.poker.common.data.remote.dto.login.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.RedirectResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType

class LoginServiceImpl(
    private val client: HttpClient,
) : LoginService {
    override suspend fun login(username: String, password: String): Resource<String> = try {
        Resource.Success(
            client.post(HttpRoutes.LOGIN) {
                contentType(ContentType.Application.Json)
                setBody(UserDto(username, password))
            }
                .body(),
        )
    } catch (e: RedirectResponseException) {
        // 3xx - responses
        Resource.Error(e.response.status.description)
    } catch (e: ClientRequestException) {
        // 4xx - responses
        Resource.Error(e.response.status.description)
    } catch (e: ServerResponseException) {
        // 5xx - responses
        Resource.Error(e.response.status.description)
    } catch (e: Exception) {
        Resource.Error(e.message ?: "An unexpected error occurred")
    }
}
