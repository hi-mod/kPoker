package com.poker.client.desktop.presentation

import com.poker.common.core.Resource
import com.poker.common.data.TokenService
import com.poker.common.data.remote.LoginService
import com.poker.common.data.remote.PokerService
import com.poker.common.domain.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PokerViewModel(
    private val coroutineScope: CoroutineScope,
    private val pokerService: PokerService,
    private val loginService: LoginService,
    private val tokenService: TokenService,
    private val appSettings: AppSettings,
) {

    private val _state = MutableStateFlow(PokerState())
    val state: StateFlow<PokerState> = combine(
        _state,
        appSettings.settingsFlow,
    ) { state, settings ->
        state.copy(
            username = settings.username ?: "",
            password = settings.password ?: "",
        )
    }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = PokerState(),
        )

    fun login() {
        coroutineScope.launch {
            val username = appSettings.username
            val password = appSettings.password
            println(username)
            if (username == null || password == null) {
                return@launch
            }
            val result = loginService.login(username, password)
            if (result is Resource.Success) {
                result.data?.let {
                    _state.update { state ->
                        state.copy(
                            loggedIn = true,
                        )
                    }
                    tokenService.saveToken(it)
                }
            }
        }
    }

    fun startGame() {
        coroutineScope.launch {
            pokerService.startGame("1").collect { game ->
                _state.update { state ->
                    state.copy(players = game.players)
                }
            }
        }
    }

    fun onEvent(event: PokerEvent) {
        when (event) {
            PokerEvent.ExitApplication -> _state.update {
                it.copy(exitApplication = true)
            }
            PokerEvent.OnLoginMenuClick -> _state.update {
                it.copy(showLogin = true)
            }
            is PokerEvent.SaveLogin -> {
                appSettings.username = event.user
                appSettings.password = event.password
            }
            PokerEvent.StartGame -> startGame()
            PokerEvent.OnDismissLogin -> _state.update {
                it.copy(showLogin = false)
            }
            PokerEvent.OnLogin -> login()
        }
    }
}
