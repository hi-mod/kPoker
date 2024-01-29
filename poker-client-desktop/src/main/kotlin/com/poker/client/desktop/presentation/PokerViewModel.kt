package com.poker.client.desktop.presentation

import com.poker.common.data.remote.PokerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PokerViewModel(
    private val coroutineScope: CoroutineScope,
    private val pokerService: PokerService,
) {

    private val _state = MutableStateFlow(PokerState())
    val state: StateFlow<PokerState> = _state

    fun startGame() {
        coroutineScope.launch {
            pokerService.startGame().collect { game ->
                _state.update { state ->
                    state.copy(players = game.players)
                }
            }
        }
    }
}
