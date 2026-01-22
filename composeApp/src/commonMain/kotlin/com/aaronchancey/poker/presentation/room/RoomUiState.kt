package com.aaronchancey.poker.presentation.room

import com.aaronchancey.poker.kpoker.betting.ActionRequest
import com.aaronchancey.poker.kpoker.betting.ShowdownRequest
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.network.ConnectionState
import com.aaronchancey.poker.shared.message.RoomInfo

data class RoomUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val playerId: PlayerId? = null,
    val handDescription: String = "",
    val roomInfo: RoomInfo? = null,
    val gameState: GameState? = null,
    val availableActions: ActionRequest? = null,
    val error: String? = null,
    val isLoading: Boolean = false,
    val showdown: ShowdownRequest? = null,
)
