package com.aaronchancey.poker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.ActionRequest
import com.aaronchancey.poker.kpoker.evaluation.HandEvaluator
import com.aaronchancey.poker.kpoker.evaluation.StandardHandEvaluator
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.network.ConnectionState
import com.aaronchancey.poker.network.PokerRepository
import com.aaronchancey.poker.shared.message.RoomInfo
import com.russhwolf.settings.Settings
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// State
data class GameUiState(
    val connectionState: ConnectionState = ConnectionState.DISCONNECTED,
    val playerId: PlayerId? = null,
    val handDescription: String = "",
    val roomInfo: RoomInfo? = null,
    val gameState: GameState? = null,
    val availableActions: ActionRequest? = null,
    val error: String? = null,
    val isLoading: Boolean = false,
)

// Intent (User Actions)
sealed interface GameIntent {
    data class Connect(val host: String, val port: Int, val roomId: String) : GameIntent
    data class JoinRoom(val playerName: String) : GameIntent
    data object LeaveRoom : GameIntent
    data class TakeSeat(val seatNumber: Int, val buyIn: ChipAmount) : GameIntent
    data object LeaveSeat : GameIntent
    data class PerformAction(val action: Action) : GameIntent
    data class SendChat(val message: String) : GameIntent
    data object Disconnect : GameIntent
    data object ClearError : GameIntent
}

// Side Effects (One-time events)
sealed interface GameEffect {
    data class ShowToast(val message: String) : GameEffect
    data object NavigateToLobby : GameEffect
    data class PlaySound(val soundType: SoundType) : GameEffect
}

enum class SoundType {
    CARD_DEAL,
    CHIP_MOVE,
    YOUR_TURN,
    WIN,
}

@OptIn(ExperimentalUuidApi::class)
class GameViewModel(
    private val settings: Settings,
    private val repository: PokerRepository = PokerRepository(),
    private val handEvaluator: HandEvaluator = StandardHandEvaluator(),
) : ViewModel() {

    private val _uiState = MutableStateFlow(GameUiState())
    val uiState: StateFlow<GameUiState> = combine(
        _uiState, // 0
        repository.connectionState, // 1
        repository.playerId, // 2
        repository.roomInfo, // 3
        repository.gameState, // 4
        repository.availableActions, // 5
        repository.error, // 6
        repository.messages.stateIn(viewModelScope, SharingStarted.Eagerly, null), // 7
        repository.errors.stateIn(viewModelScope, SharingStarted.Eagerly, null), // 8
    ) { values ->
        val state = values[0] as GameUiState
        val connectionState = values[1] as ConnectionState
        val playerId = values[2] as PlayerId?
            ?: settings.getString("playerId", Uuid.generateV4().toString())
        val roomInfo = values[3] as RoomInfo?
        val gameState = values[4] as GameState?
        val availableActions = values[5] as ActionRequest?
        val error = values[6] as String?

        settings.putString("playerId", playerId)
        val cards = gameState?.communityCards?.plus(
            gameState.activePlayers.firstOrNull { it.player.id == playerId }?.holeCards
                ?: emptyList(),
        )
            ?: emptyList()
        val handDescription = if (cards.size >= 5) {
            val hand = handEvaluator.findBestHand(cards)
            hand.description()
        } else {
            ""
        }
        state.copy(
            connectionState = connectionState,
            handDescription = handDescription,
            playerId = playerId,
            roomInfo = roomInfo,
            gameState = gameState,
            availableActions = availableActions,
            error = error,
        )
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = GameUiState(),
        )

    private val _effects = Channel<GameEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: GameIntent) = when (intent) {
        is GameIntent.Connect -> handleConnect(intent.host, intent.port, intent.roomId, uiState.value.playerId!!)
        is GameIntent.JoinRoom -> handleJoinRoom(intent.playerName)
        is GameIntent.LeaveRoom -> handleLeaveRoom()
        is GameIntent.TakeSeat -> handleTakeSeat(intent.seatNumber, intent.buyIn)
        is GameIntent.LeaveSeat -> handleLeaveSeat()
        is GameIntent.PerformAction -> handlePerformAction(intent.action)
        is GameIntent.SendChat -> handleSendChat(intent.message)
        is GameIntent.Disconnect -> handleDisconnect()
        is GameIntent.ClearError -> handleClearError()
    }

    private fun handleConnect(
        host: String,
        port: Int,
        roomId: String,
        playerId: PlayerId,
    ) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        try {
            repository.connect(host, port, roomId, playerId)
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun handleJoinRoom(playerName: String) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        try {
            repository.joinRoom(playerName)
        } finally {
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun handleLeaveRoom() = viewModelScope.launch {
        repository.leaveRoom()
        _effects.send(GameEffect.NavigateToLobby)
    }

    private fun handleTakeSeat(seatNumber: Int, buyIn: ChipAmount) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.takeSeat(seatNumber, buyIn)
                _effects.send(GameEffect.PlaySound(SoundType.CHIP_MOVE))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun handleLeaveSeat() = viewModelScope.launch {
        repository.leaveSeat()
    }

    private fun handlePerformAction(action: Action) = viewModelScope.launch {
        repository.performAction(action)
        _effects.send(GameEffect.PlaySound(SoundType.CHIP_MOVE))
    }

    private fun handleSendChat(message: String) = viewModelScope.launch {
        repository.sendChat(message)
    }

    private fun handleDisconnect() = viewModelScope.launch {
        repository.disconnect()
        _effects.send(GameEffect.NavigateToLobby)
    }

    private fun handleClearError() {
        repository.clearError()
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}
