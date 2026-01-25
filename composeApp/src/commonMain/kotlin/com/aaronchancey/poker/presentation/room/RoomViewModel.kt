package com.aaronchancey.poker.presentation.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaronchancey.poker.config.AppConfig
import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.ActionRequest
import com.aaronchancey.poker.kpoker.betting.ShowdownRequest
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * ViewModel for managing a player's session within a poker room.
 *
 * Handles:
 * - Room joining/leaving with session persistence
 * - Auto-reconnection with automatic room rejoin
 * - Seat management and game actions
 * - Hand evaluation display
 */
class RoomViewModel(
    private val params: RoomParams,
    private val settings: Settings,
    private val repository: PokerRepository,
) : ViewModel() {

    /** Guards against multiple join attempts when flow is resubscribed. */
    private var joinStarted = false

    /** Guards against multiple connection observers. */
    private var observationStarted = false

    private val _uiState = MutableStateFlow(RoomUiState())
    val uiState: StateFlow<RoomUiState> = combine(
        _uiState, // 0
        repository.connectionState, // 1
        repository.playerId, // 2
        repository.roomInfo, // 3
        repository.gameState, // 4
        repository.availableActions, // 5
        repository.showDown, // 6
        repository.error, // 7
        repository.messages.stateIn(viewModelScope, SharingStarted.Eagerly, null), // 8
        repository.errors.stateIn(viewModelScope, SharingStarted.Eagerly, null), // 9
    ) { values ->
        val state = values[0] as RoomUiState
        val connectionState = values[1] as ConnectionState
        val playerId = values[2] as PlayerId? ?: params.playerId
        val roomInfo = values[3] as RoomInfo?
        val gameState = values[4] as GameState?
        val availableActions = values[5] as ActionRequest?
        val showDown = values[6] as ShowdownRequest?
        val error = values[7] as String?

        settings.putString(KEY_PLAYER_ID, playerId)

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
            showdown = showDown,
            error = error,
        )
    }
        .onStart { startConnectionObservation() }
        .onStart { joinRoom() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = RoomUiState(),
        )

    private val _effects = Channel<RoomEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    fun onIntent(intent: RoomIntent) = when (intent) {
        is RoomIntent.LeaveRoom -> handleLeaveRoom()
        is RoomIntent.TakeSeat -> handleTakeSeat(intent.seatNumber, intent.buyIn)
        is RoomIntent.LeaveSeat -> handleLeaveSeat()
        is RoomIntent.PerformAction -> handlePerformAction(intent.action)
        is RoomIntent.SendChat -> handleSendChat(intent.message)
        is RoomIntent.Disconnect -> handleDisconnect()
        is RoomIntent.ClearError -> handleClearError()
    }

    /**
     * Starts observing connection state to handle auto-rejoin after reconnection.
     * Called lazily when uiState is first collected via onStart operator.
     */
    private fun startConnectionObservation() {
        if (observationStarted) return
        observationStarted = true
        println("[RoomViewModel] startConnectionObservation: Starting for roomId=${params.roomId}")
        viewModelScope.launch {
            repository.connectionState.collect { state ->
                println("[RoomViewModel] connectionState changed: $state")
                if (state == ConnectionState.RECONNECTED) {
                    handleAutoRejoin()
                }
            }
        }
    }

    /**
     * Joins the room specified in [params].
     * Called lazily when uiState is first collected via onStart operator.
     */
    private fun joinRoom() {
        if (joinStarted) return
        joinStarted = true
        println("[RoomViewModel] joinRoom: Starting - roomId=${params.roomId}, playerName=${params.playerName}, playerId=${params.playerId}")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                println("[RoomViewModel] joinRoom: Connecting to ${AppConfig.wsHost}:${AppConfig.wsPort}")
                repository.connect(AppConfig.wsHost, AppConfig.wsPort, params.roomId, params.playerId)
                println("[RoomViewModel] joinRoom: Waiting for connection...")
                withTimeout(10.seconds) {
                    repository.connectionState.first { it == ConnectionState.CONNECTED }
                }
                println("[RoomViewModel] joinRoom: Connected! Sending JoinRoom message")
                repository.joinRoom(params.playerName)
                saveSession(params.roomId, params.playerName)
                println("[RoomViewModel] joinRoom: Successfully joined room")
            } catch (e: Exception) {
                println("[RoomViewModel] joinRoom: FAILED - ${e::class.simpleName}: ${e.message}")
                // Surface the error to the user
                _uiState.update { it.copy(error = "Failed to join room: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Auto-rejoin the room after a reconnection.
     * Uses saved session data to restore the player's state.
     */
    private suspend fun handleAutoRejoin() {
        println("[RoomViewModel] handleAutoRejoin: Triggered for roomId=${params.roomId}")
        val playerName = settings.getStringOrNull(KEY_PLAYER_NAME)
        val playerId = settings.getStringOrNull(KEY_PLAYER_ID)
        println("[RoomViewModel] handleAutoRejoin: savedPlayerName=$playerName, savedPlayerId=$playerId")
        if (playerName != null) {
            println("[RoomViewModel] handleAutoRejoin: Sending JoinRoom with playerName=$playerName")
            repository.joinRoom(playerName)
        } else {
            println("[RoomViewModel] handleAutoRejoin: No saved playerName, skipping JoinRoom")
        }
        repository.acknowledgeReconnected()
        println("[RoomViewModel] handleAutoRejoin: Acknowledged reconnection")
    }

    private fun handleLeaveRoom() = viewModelScope.launch {
        repository.leaveRoom()
        clearSession()
        _effects.send(RoomEffect.NavigateToLobby)
    }

    private fun handleTakeSeat(seatNumber: Int, buyIn: ChipAmount) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.takeSeat(seatNumber, buyIn)
                _effects.send(RoomEffect.PlaySound(SoundType.CHIP_MOVE))
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
        when (action) {
            is Action.Check -> SoundType.CHECK
            is Action.Fold -> null
            else -> SoundType.CHIP_MOVE
        }?.let { soundType ->
            _effects.send(RoomEffect.PlaySound(soundType))
        }
    }

    private fun handleSendChat(message: String) = viewModelScope.launch {
        repository.sendChat(message)
    }

    private fun handleDisconnect() = viewModelScope.launch {
        repository.disconnect()
        clearSession()
        _effects.send(RoomEffect.NavigateToLobby)
    }

    private fun handleClearError() {
        repository.clearError()
    }

    /**
     * Saves the current session info to Settings.
     * This allows the app to restore the session after a restart.
     */
    private fun saveSession(roomId: String, playerName: String) {
        val roomName = uiState.value.roomInfo?.roomName
        println("[RoomViewModel] saveSession: roomId=$roomId, playerName=$playerName, roomName=$roomName")
        settings.putString(KEY_CURRENT_ROOM_ID, roomId)
        settings.putString(KEY_PLAYER_NAME, playerName)
        // Room name is set when we receive RoomJoined message
        roomName?.let {
            settings.putString(KEY_CURRENT_ROOM_NAME, it)
        }
        println("[RoomViewModel] saveSession: Saved session to settings")
    }

    /**
     * Clears the saved session info from Settings.
     * Called when the player intentionally leaves or disconnects.
     */
    private fun clearSession() {
        settings.remove(KEY_CURRENT_ROOM_ID)
        settings.remove(KEY_CURRENT_ROOM_NAME)
        // Note: playerName is kept for convenience on next join
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }

    companion object {
        const val KEY_PLAYER_ID = "playerId"
        const val KEY_PLAYER_NAME = "playerName"
        const val KEY_CURRENT_ROOM_ID = "currentRoomId"
        const val KEY_CURRENT_ROOM_NAME = "currentRoomName"
    }
}
