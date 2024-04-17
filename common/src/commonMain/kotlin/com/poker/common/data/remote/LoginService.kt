package com.poker.common.data.remote

import com.poker.common.core.Resource

interface LoginService {
    suspend fun login(username: String, password: String): Resource<String>
}
