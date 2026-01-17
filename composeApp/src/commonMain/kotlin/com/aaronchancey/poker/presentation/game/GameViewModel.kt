package com.aaronchancey.poker.presentation.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaronchancey.poker.config.AppConfig
import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.ActionRequest
import com.aaronchancey.poker.kpoker.evaluation.HandEvaluatorFactory
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.game.GameVariant
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.network.ConnectionState
import com.aaronchancey.poker.network.PokerRepository
import com.aaronchancey.poker.shared.message.RoomInfo
import com.russhwolf.settings.Settings
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalUuidApi::class)
class GameViewModel(
    private val settings: Settings,
    private val repository: PokerRepository = PokerRepository(),
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

        val communityCards = gameState?.communityCards ?: emptyList()
        val holeCards = gameState?.activePlayers?.firstOrNull { it.player.id == playerId }?.holeCards
            ?: emptyList()

        val handDescription = if (holeCards.isNotEmpty()) {
            try {
                val variant = gameState?.variant ?: GameVariant.TEXAS_HOLDEM
                val evaluator = HandEvaluatorFactory.getEvaluator(variant)

                if (communityCards.size >= 3) {
                    // Post-flop: full hand evaluation
                    val bestHands = if (variant == GameVariant.TEXAS_HOLDEM) {
                        val allCards = holeCards + communityCards
                        if (allCards.size >= 5) {
                            evaluator.findBestHand(allCards, 5)
                        } else {
                            emptyList()
                        }
                    } else {
                        if (holeCards.size >= 2 && communityCards.size >= 3) {
                            evaluator.findBestHand(holeCards, communityCards)
                        } else {
                            emptyList()
                        }
                    }
                    bestHands.joinToString(", ") { it.description() }
                } else {
                    // Pre-flop: partial evaluation of hole cards only
                    evaluator.evaluatePartial(holeCards)?.description() ?: ""
                }
            } catch (_: Exception) {
                ""
            }
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
        is GameIntent.JoinRoom -> handleJoinRoom(intent)
        is GameIntent.LeaveRoom -> handleLeaveRoom()
        is GameIntent.TakeSeat -> handleTakeSeat(intent.seatNumber, intent.buyIn)
        is GameIntent.LeaveSeat -> handleLeaveSeat()
        is GameIntent.PerformAction -> handlePerformAction(intent.action)
        is GameIntent.SendChat -> handleSendChat(intent.message)
        is GameIntent.Disconnect -> handleDisconnect()
        is GameIntent.ClearError -> handleClearError()
    }

    private fun handleJoinRoom(intent: GameIntent.JoinRoom) = viewModelScope.launch {
        _uiState.update { it.copy(isLoading = true) }
        try {
            val host = AppConfig.wsHost
            val port = AppConfig.wsPort
            repository.connect(host, port, intent.roomId, uiState.value.playerId!!)
            withTimeout(10.seconds) {
                repository.connectionState.first { it == ConnectionState.CONNECTED }
            }
            repository.joinRoom(intent.playerName)
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
