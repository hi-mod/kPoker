package com.aaronchancey.poker.presentation.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaronchancey.poker.config.AppConfig
import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.events.GameEvent
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.network.ConnectionState
import com.aaronchancey.poker.network.PokerRepository
import com.aaronchancey.poker.presentation.util.SettingKeys.KEY_CURRENT_ROOM_ID
import com.aaronchancey.poker.presentation.util.SettingKeys.KEY_CURRENT_ROOM_NAME
import com.aaronchancey.poker.presentation.util.SettingKeys.KEY_PLAYER_ID
import com.aaronchancey.poker.presentation.util.SettingKeys.KEY_PLAYER_NAME
import com.russhwolf.settings.Settings
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Local UI state managed entirely by the ViewModel.
 * Separate from server-driven session state.
 */
private data class LocalUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
)

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
    private val handDescriptionProvider: HandDescriptionProvider,
) : ViewModel() {

    /** Guards against multiple join attempts when flow is resubscribed. */
    private var joinStarted = false

    /** Guards against multiple connection observers. */
    private var observationStarted = false

    /** Tracks previous bets for chip animation detection. */
    private var previousBets: Map<Int, Double> = emptyMap()

    private val localState = MutableStateFlow(LocalUiState())

    val uiState: StateFlow<RoomUiState> = combine(
        localState,
        repository.session,
        repository.connectionState,
    ) { localState, session, connectionState ->
        RoomUiState(
            connectionState = connectionState,
            playerId = session.playerId ?: params.playerId,
            handDescription = handDescriptionProvider.getHandDescription(
                gameState = session.gameState,
                playerId = session.playerId ?: params.playerId,
            ),
            roomInfo = session.roomInfo,
            gameState = session.gameState,
            availableActions = session.availableActions,
            showdown = session.showdown,
            error = session.error ?: localState.error,
            isLoading = localState.isLoading,
        )
    }
        .onStart { startConnectionObservation() }
        .onStart { startSettingsPersistence() }
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
        startChipAnimationObservation()
        viewModelScope.launch {
            repository.gameEvents
                .filterIsInstance<GameEvent.HandStarted>()
                .collect {
                    _effects.send(RoomEffect.PlaySound(SoundType.CARD_DEAL))
                }
        }
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
     * Persists session data to Settings when relevant state changes.
     * Only writes when values actually change, avoiding wasteful writes on every combine.
     */
    private fun startSettingsPersistence() {
        viewModelScope.launch {
            // Persist playerId when it changes
            repository.session
                .map { it.playerId }
                .distinctUntilChanged()
                .collect { playerId ->
                    if (playerId != null) {
                        settings.putString(KEY_PLAYER_ID, playerId)
                    }
                }
        }
        viewModelScope.launch {
            // Persist roomName when it changes
            repository.session
                .map { it.roomInfo?.roomName }
                .distinctUntilChanged()
                .collect { roomName ->
                    if (roomName != null) {
                        settings.putString(KEY_CURRENT_ROOM_NAME, roomName)
                    }
                }
        }
    }

    /**
     * Observes game state changes to detect when bets should animate to the pot.
     * Emits [RoomEffect.AnimateChipsToPot] when bets clear (phase transition).
     */
    private fun startChipAnimationObservation() {
        viewModelScope.launch {
            repository.session
                .map { it.gameState }
                .collect { gameState ->
                    val currentBets = gameState?.table?.seats
                        ?.mapNotNull { seat ->
                            val bet = seat.playerState?.currentBet ?: 0.0
                            if (bet > 0.0) seat.number to bet else null
                        }
                        ?.toMap() ?: emptyMap()

                    // Find bets that existed before but are now zero (cleared)
                    val clearedBets = previousBets.filter { (seat, _) ->
                        currentBets[seat] == null || currentBets[seat] == 0.0
                    }

                    if (clearedBets.isNotEmpty()) {
                        _effects.send(
                            RoomEffect.AnimateChipsToPot(
                                clearedBets.map { AnimatingBet(it.key, it.value) },
                            ),
                        )
                    }

                    previousBets = currentBets
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
            localState.update { it.copy(isLoading = true) }
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
                localState.update { it.copy(error = "Failed to join room: ${e.message}") }
            } finally {
                localState.update { it.copy(isLoading = false) }
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
            localState.update { it.copy(isLoading = true) }
            try {
                repository.takeSeat(seatNumber, buyIn)
                _effects.send(RoomEffect.PlaySound(SoundType.CHIP_MOVE))
            } finally {
                localState.update { it.copy(isLoading = false) }
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
        localState.update { it.copy(error = null) }
    }

    /**
     * Saves the current session info to Settings.
     * This allows the app to restore the session after a restart.
     */
    private fun saveSession(roomId: String, playerName: String) {
        println("[RoomViewModel] saveSession: roomId=$roomId, playerName=$playerName")
        settings.putString(KEY_CURRENT_ROOM_ID, roomId)
        settings.putString(KEY_PLAYER_NAME, playerName)
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
}
