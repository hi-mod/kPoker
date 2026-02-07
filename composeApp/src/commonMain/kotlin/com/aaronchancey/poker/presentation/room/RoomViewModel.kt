package com.aaronchancey.poker.presentation.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaronchancey.poker.config.AppConfig
import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.ActionRequest
import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.equity.ActionEv
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
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
    val selectedPreAction: PreActionType? = null,
    /** The bet amount when the pre-action was selected, used for invalidation. */
    val preActionSnapshotBet: ChipAmount = 0.0,
    /** Computed equity and action EVs, updated when board or actions change. */
    val actionEv: ActionEv? = null,
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
    private val actionEvProvider: ActionEvProvider,
) : ViewModel() {

    /** Guards against multiple join attempts when flow is resubscribed. */
    private var joinStarted = false

    /** Guards against multiple connection observers. */
    private var observationStarted = false

    /** Tracks previous bets for chip animation detection. */
    private var previousBets: Map<Int, Double> = emptyMap()

    /** Tracks previous winners for pot-to-player animation detection. */
    private var previousWinners: List<com.aaronchancey.poker.kpoker.game.Winner> = emptyList()

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
            selectedPreAction = localState.selectedPreAction,
            actionEv = localState.actionEv,
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

    private val _effects = MutableSharedFlow<RoomEffect>(extraBufferCapacity = 10)
    val effects = _effects.asSharedFlow()

    fun onIntent(intent: RoomIntent) = when (intent) {
        is RoomIntent.LeaveRoom -> handleLeaveRoom()
        is RoomIntent.TakeSeat -> handleTakeSeat(intent.seatNumber, intent.buyIn)
        is RoomIntent.LeaveSeat -> handleLeaveSeat()
        is RoomIntent.PerformAction -> handlePerformAction(intent.action)
        is RoomIntent.ToggleSitOut -> handleToggleSitOut()
        is RoomIntent.SelectPreAction -> handleSelectPreAction(intent.preAction)
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
        observeGameState()
        observePreActionAutoSubmit()
        observeEquity()
        viewModelScope.launch {
            repository.gameEvents
                .filterIsInstance<GameEvent.HandStarted>()
                .collect {
                    _effects.emit(RoomEffect.PlaySound(SoundType.CARD_DEAL))
                    _effects.emit(
                        RoomEffect.DealCards(
                            uiState.value.gameState
                                ?.activePlayers
                                ?.firstOrNull { it.holeCards.isNotEmpty() }
                                ?.holeCards?.size ?: 0,
                        ),
                    )
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
     * Observes game state changes to detect when chips should animate.
     * Emits [RoomEffect.AnimateChipsToPot] when bets clear (phase transition).
     * Emits [RoomEffect.AnimateChipsFromPot] when winners are declared.
     */
    private fun observeGameState() = viewModelScope.launch {
        repository.session
            .map { it.gameState }
            .collect { gameState ->
                // === Bet clearing detection (bets → pot) ===
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
                val currentWinners = gameState?.winners ?: emptyList()
                val winnersJustDeclared = previousWinners.isEmpty() && currentWinners.isNotEmpty()

                when {
                    clearedBets.isNotEmpty() -> {
                        _effects.emit(
                            RoomEffect.AnimateChipsToPot(
                                clearedBets.map { AnimatingBet(it.key, it.value) },
                            ),
                        )
                    }

                    winnersJustDeclared && gameState != null -> {
                        // Brief delay to let bet animations finish first
                        delay(300)

                        val winnings = currentWinners.mapNotNull { winner ->
                            gameState.table.getPlayerSeat(winner.playerId)?.number?.let { seat ->
                                AnimatingBet(seat, winner.amount)
                            }
                        }

                        if (winnings.isNotEmpty()) {
                            _effects.emit(RoomEffect.AnimateChipsFromPot(winnings))
                        }
                    }
                }

                previousBets = currentBets
                previousWinners = currentWinners
            }
    }

    /**
     * Observes community cards and available actions to recompute equity and EV.
     * Uses [distinctUntilChanged] on a composite key to avoid redundant Monte Carlo runs.
     */
    private fun observeEquity() = viewModelScope.launch {
        data class EquityKey(
            val communityCards: List<Card>,
            val availableActions: ActionRequest?,
        )

        repository.session
            .map { session ->
                EquityKey(
                    communityCards = session.gameState?.communityCards ?: emptyList(),
                    availableActions = session.availableActions,
                )
            }
            .distinctUntilChanged()
            .collect {
                val session = repository.session.value
                val playerId = session.playerId ?: params.playerId
                val actionEv = actionEvProvider.getActionEv(
                    gameState = session.gameState,
                    playerId = playerId,
                    availableActions = session.availableActions,
                )
                localState.update { it.copy(actionEv = actionEv) }
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
        _effects.emit(RoomEffect.NavigateToLobby)
    }

    private fun handleTakeSeat(seatNumber: Int, buyIn: ChipAmount) {
        viewModelScope.launch {
            localState.update { it.copy(isLoading = true) }
            try {
                repository.takeSeat(seatNumber, buyIn)
                _effects.emit(RoomEffect.PlaySound(SoundType.CHIP_MOVE))
            } finally {
                localState.update { it.copy(isLoading = false) }
            }
        }
    }

    private fun handleLeaveSeat() = viewModelScope.launch {
        repository.leaveSeat()
    }

    private fun handlePerformAction(action: Action) = viewModelScope.launch {
        // Clear any pre-action when player manually acts
        localState.update { it.copy(selectedPreAction = null, preActionSnapshotBet = 0.0) }
        repository.performAction(action)
        when (action) {
            is Action.Check -> SoundType.CHECK
            is Action.Fold -> null
            else -> SoundType.CHIP_MOVE
        }?.let { soundType ->
            _effects.emit(RoomEffect.PlaySound(soundType))
        }
    }

    /**
     * Observes when it becomes the player's turn and auto-submits a pre-action if valid.
     * Clears pre-action on hand end.
     */
    private fun observePreActionAutoSubmit() {
        // Auto-submit pre-action when ActionRequired arrives for this player
        viewModelScope.launch {
            repository.session
                .map { it.availableActions }
                .distinctUntilChanged()
                .collect { actionRequest ->
                    if (actionRequest == null) return@collect
                    val local = localState.value
                    val preAction = local.selectedPreAction ?: return@collect

                    println("[PreAction] Resolving $preAction (snapshot=${local.preActionSnapshotBet}, amountToCall=${actionRequest.amountToCall})")

                    val resolved = resolvePreAction(
                        preAction = preAction,
                        actionRequest = actionRequest,
                        snapshotBet = local.preActionSnapshotBet,
                    )

                    // Clear pre-action regardless of whether it resolved
                    localState.update { it.copy(selectedPreAction = null, preActionSnapshotBet = 0.0) }

                    if (resolved != null) {
                        println("[PreAction] Auto-submitting: $resolved")
                        repository.performAction(resolved)
                    } else {
                        println("[PreAction] Invalidated - showing normal action buttons")
                    }
                }
        }

        // Clear pre-action on hand end
        viewModelScope.launch {
            repository.gameEvents
                .filterIsInstance<GameEvent.HandComplete>()
                .collect {
                    localState.update { it.copy(selectedPreAction = null, preActionSnapshotBet = 0.0) }
                }
        }
    }

    private fun handleSelectPreAction(preAction: PreActionType?) {
        val session = repository.session.value
        val gameState = session.gameState
        val playerId = session.playerId

        // Snapshot the amount-to-call at selection time for CALL invalidation.
        // This is what the player would currently need to put in to call.
        val roundBet = gameState?.bettingRound?.currentBet ?: 0.0
        val myCurrentBet = playerId?.let {
            gameState?.table?.getPlayerSeat(it)?.playerState?.currentBet
        } ?: 0.0
        val snapshotAmountToCall = maxOf(0.0, roundBet - myCurrentBet)

        localState.update {
            it.copy(
                selectedPreAction = preAction,
                preActionSnapshotBet = snapshotAmountToCall,
            )
        }
    }

    private fun handleToggleSitOut() = viewModelScope.launch {
        repository.toggleSitOut()
    }

    private fun handleSendChat(message: String) = viewModelScope.launch {
        repository.sendChat(message)
    }

    private fun handleDisconnect() = viewModelScope.launch {
        repository.disconnect()
        clearSession()
        _effects.emit(RoomEffect.NavigateToLobby)
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
