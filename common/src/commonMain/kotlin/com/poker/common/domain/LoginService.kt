package com.poker.common.domain

import com.poker.common.core.Resource

interface LoginService {
    suspend fun login(username: String, password: String): Resource<String>
}
