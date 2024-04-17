package com.poker.common.data

class TokenService {
    private var _token: String? = null
    val token: String?
        get() = _token

    fun saveToken(token: String) {
        _token = token
    }
}
