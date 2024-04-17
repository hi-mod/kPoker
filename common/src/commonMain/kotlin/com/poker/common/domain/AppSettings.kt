package com.poker.common.domain

import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class AppSettings(private val settings: Settings) {
    private val _settingsFlow = MutableStateFlow(this)
    val settingsFlow: StateFlow<AppSettings> = _settingsFlow

    var username: String?
        get() = settings[USERNAME]
        set(value) {
            settings[USERNAME] = value
            _settingsFlow.update { this }
        }

    var password: String?
        get() = settings[PASSWORD]
        set(value) {
            settings[PASSWORD] = value
            _settingsFlow.update { this }
        }

    private companion object {
        private const val USERNAME = "username"
        private const val PASSWORD = "password"
    }
}
