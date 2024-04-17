package com.poker.common.data.remote.dto.login

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val username: String,
    val password: String,
)
