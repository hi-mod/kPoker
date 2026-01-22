package com.aaronchancey.poker.room

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.ActionRequest
import com.aaronchancey.poker.kpoker.betting.ShowdownRequest
import com.aaronchancey.poker.kpoker.events.GameEvent
import com.aaronchancey.poker.kpoker.game.GamePhase
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.game.PokerGame
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.Seat
import com.aaronchancey.poker.kpoker.player.Table
import com.aaronchancey.poker.kpoker.variants.OmahaGame
import com.aaronchancey.poker.kpoker.variants.TexasHoldemGame
import com.aaronchancey.poker.kpoker.visibility.StandardVisibility
import com.aaronchancey.poker.kpoker.visibility.VisibilityService
import com.aaronchancey.poker.persistence.RoomStateData
import com.aaronchancey.poker.shared.message.RoomInfo
import com.aaronchancey.poker.shared.message.ServerMessage
import com.aaronchancey.poker.shared.model.GameVariant
import com.aaronchancey.poker.ws.ConnectionManager
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Server-side room that manages a poker game instance.
 *
 * Responsibilities:
 * - Game lifecycle (wraps PokerGame)
 * - Spectator management
 * - Seat reservations (via SeatManager)
 * - Visibility filtering (via VisibilityService)
 * - Broadcasting state to connected clients
 */
class ServerRoom(
    val config: RoomConfig,
    private val connectionManager: ConnectionManager,
    private val visibilityService: VisibilityService = StandardVisibility(),
    initialGameState: GameState? = null,
) {
    // Convenience accessors for backwards compatibility
    val roomId: String get() = config.roomId
    val roomName: String get() = config.roomName
    val maxPlayers: Int get() = config.maxPlayers
    val smallBlind: ChipAmount get() = config.smallBlind
    val bigBlind: ChipAmount get() = config.bigBlind
    val minBuyIn: ChipAmount get() = config.minBuyIn
    val maxBuyIn: ChipAmount get() = config.maxBuyIn
    val minDenomination: ChipAmount get() = config.minDenomination
    val variant: GameVariant get() = config.variant

    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.Default)
    private val spectators = mutableMapOf<PlayerId, RoomParticipant.Spectator>()
    private val seatManager = SeatManager(config.maxPlayers, config.reservationDurationMs)
    private var game: PokerGame = createGame()

    val spectatorCount: Int get() = spectators.size
    val playerCount: Int get() = game.currentState.table.playerCount

    val participants: List<RoomParticipant>
        get() = spectators.values.toList() +
            game.currentState.table.occupiedSeats.mapNotNull { seat ->
                seat.playerState?.let { ps ->
                    RoomParticipant.SeatedPlayer(ps.player, seat.number, ps.chips)
                }
            }

    init {
        if (initialGameState != null) {
            game.restoreState(initialGameState)
        } else {
            game.initialize(Table.create(config.roomId, config.roomName, config.maxPlayers))
        }
        setupEventListeners()
    }

    /** Secondary constructor for backwards compatibility. */
    constructor(
        roomId: String,
        roomName: String,
        minDenomination: ChipAmount = 0.1,
        maxPlayers: Int = 9,
        smallBlind: ChipAmount = 1.0,
        bigBlind: ChipAmount = 2.0,
        minBuyIn: ChipAmount = 40.0,
        maxBuyIn: ChipAmount = 200.0,
        variant: GameVariant = GameVariant.TEXAS_HOLDEM_NL,
        connectionManager: ConnectionManager,
        initialGameState: GameState? = null,
    ) : this(
        config = RoomConfig(
            roomId = roomId,
            roomName = roomName,
            maxPlayers = maxPlayers,
            smallBlind = smallBlind,
            bigBlind = bigBlind,
            minBuyIn = minBuyIn,
            maxBuyIn = maxBuyIn,
            minDenomination = minDenomination,
            variant = variant,
        ),
        connectionManager = connectionManager,
        initialGameState = initialGameState,
    )

    private fun createGame(): PokerGame = when (config.variant) {
        GameVariant.OMAHA_PL -> OmahaGame.potLimit(smallBlind, bigBlind, minDenomination)
        GameVariant.OMAHA_HILO_PL -> OmahaGame.potLimitHiLo(smallBlind, bigBlind, minDenomination)
        GameVariant.TEXAS_HOLDEM_NL -> TexasHoldemGame.noLimit(smallBlind, bigBlind, minDenomination)
    }

    private fun setupEventListeners() {
        game.addEventListener { event ->
            scope.launch {
                connectionManager.broadcast(roomId, ServerMessage.GameEventOccurred(event))
                broadcastVisibleGameState()

                when (event) {
                    is GameEvent.TurnChanged -> {
                        // Check if we're in showdown phase - send ShowdownRequired instead of ActionRequired
                        if (game.currentState.phase == GamePhase.SHOWDOWN) {
                            game.getShowdownRequest()?.let { showdownRequest ->
                                connectionManager.sendTo(roomId, event.playerId, ServerMessage.ShowdownRequired(showdownRequest))
                            }
                        } else {
                            game.getActionRequest()?.let { actionRequest ->
                                connectionManager.sendTo(roomId, event.playerId, ServerMessage.ActionRequired(actionRequest))
                            }
                        }
                    }

                    is GameEvent.HandComplete -> {
                        delay(5.seconds)
                        startHandIfReady()
                    }

                    else -> Unit
                }
            }
        }
    }

    // === Spectator Management ===

    suspend fun joinAsSpectator(player: Player): Result<Unit> = mutex.withLock {
        when {
            !config.allowSpectators -> Result.failure(IllegalStateException("Spectators not allowed"))

            spectators.size >= config.maxSpectators -> Result.failure(IllegalStateException("Spectator limit reached"))

            isPlayerInRoom(player.id) -> Result.failure(IllegalStateException("Player already in room"))

            else -> {
                spectators[player.id] = RoomParticipant.Spectator(player)
                Result.success(Unit)
            }
        }
    }

    fun isPlayerInRoom(playerId: PlayerId): Boolean = spectators.containsKey(playerId) || game.currentState.table.getPlayerSeat(playerId) != null

    // === Seat Reservations ===

    suspend fun getAvailableSeats(currentTime: Long = System.currentTimeMillis()): List<Int> = mutex.withLock {
        seatManager.getAvailableSeats(game.currentState.table, currentTime)
    }

    suspend fun getSeatInfo(currentTime: Long = System.currentTimeMillis()): List<SeatInfo> = mutex.withLock {
        seatManager.getSeatInfo(game.currentState.table, currentTime)
    }

    suspend fun reserveSeat(playerId: PlayerId, seatNumber: Int, currentTime: Long = System.currentTimeMillis()): SeatSelectionResult = mutex.withLock {
        when {
            !isPlayerInRoom(playerId) -> SeatSelectionResult.PlayerNotInRoom(playerId)

            game.currentState.table.getPlayerSeat(playerId) != null ->
                SeatSelectionResult.AlreadySeated(game.currentState.table.getPlayerSeat(playerId)!!.number)

            else -> seatManager.reserveSeat(seatNumber, playerId, game.currentState.table, currentTime)
        }
    }

    // === Seating ===

    suspend fun seatPlayer(
        playerId: PlayerId,
        playerName: String,
        seatNumber: Int,
        buyIn: ChipAmount,
        currentTime: Long = System.currentTimeMillis(),
    ): Result<Unit> = mutex.withLock {
        try {
            if (buyIn < config.minBuyIn) {
                return@withLock Result.failure(IllegalArgumentException("Buy-in below minimum: ${config.minBuyIn}"))
            }
            val actualBuyIn = minOf(buyIn, config.maxBuyIn)

            val seat = game.currentState.table.getSeat(seatNumber)
            if (seat?.isOccupied == true) {
                return@withLock Result.failure(IllegalStateException("Seat occupied"))
            }

            val hasReservation = seatManager.hasReservation(seatNumber, playerId, currentTime)
            val seatInfo = seatManager.getSeatInfo(game.currentState.table, currentTime).find { it.number == seatNumber }
            if (seatInfo?.status == SeatStatus.RESERVED && !hasReservation) {
                return@withLock Result.failure(IllegalStateException("Seat reserved by another player"))
            }

            spectators.remove(playerId)
            seatManager.consumeReservation(seatNumber, playerId)

            val player = Player(playerId, playerName)
            val newTable = game.currentState.table.sitPlayer(seatNumber, PlayerState(player, actualBuyIn))
            game.updateTable(newTable)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun standPlayer(playerId: PlayerId): Result<ChipAmount> = mutex.withLock {
        try {
            val seat = game.currentState.table.getPlayerSeat(playerId)
                ?: return@withLock Result.failure(IllegalStateException("Player not seated"))

            val chips = seat.playerState?.chips ?: 0.0
            val player = seat.playerState?.player

            game.updateTable(game.currentState.table.standPlayer(playerId))
            seatManager.cancelReservation(playerId)

            if (player != null && config.allowSpectators) {
                spectators[playerId] = RoomParticipant.Spectator(player)
            }

            Result.success(chips)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // === Game Operations ===

    suspend fun performAction(playerId: PlayerId, action: Action): Result<Unit> = mutex.withLock {
        try {
            require(action.playerId == playerId) { "Action playerId mismatch" }

            // Route to appropriate handler based on action type
            when (action) {
                is Action.Show, is Action.Muck, is Action.Collect -> game.processShowdownAction(action)
                else -> game.processAction(action)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun startHandIfReady(): Boolean = mutex.withLock {
        if (game.currentState.table.playerCount >= 2 && !game.currentState.isHandInProgress) {
            game.startHand()
            true
        } else {
            false
        }
    }

    // === Broadcasting ===

    suspend fun broadcastVisibleGameState() {
        val fullState = game.currentState
        connectionManager.getConnections(roomId).forEach { connection ->
            val visibleState = if (isPlayerSeated(connection.playerId)) {
                visibilityService.getVisibleState(fullState, connection.playerId)
            } else {
                visibilityService.getSpectatorView(fullState)
            }
            connectionManager.sendTo(roomId, connection.playerId, ServerMessage.GameStateUpdate(visibleState))
        }
    }

    // === Query Methods ===

    fun isPlayerSeated(playerId: PlayerId): Boolean = game.currentState.table.getPlayerSeat(playerId) != null
    fun getPlayerSeat(playerId: PlayerId): Seat? = game.currentState.table.getPlayerSeat(playerId)
    fun getGameState(): GameState = game.currentState
    fun getActionRequest(): ActionRequest? = game.getActionRequest()
    fun getShowdownRequest(): ShowdownRequest? = game.getShowdownRequest()
    fun addGameEventListener(listener: (GameEvent) -> Unit) = game.addEventListener(listener)

    // === Info Methods ===

    fun getRoomInfo(): RoomInfo = RoomInfo(
        roomId = config.roomId,
        roomName = config.roomName,
        maxPlayers = config.maxPlayers,
        smallBlind = config.smallBlind,
        bigBlind = config.bigBlind,
        minBuyIn = config.minBuyIn,
        maxBuyIn = config.maxBuyIn,
        playerCount = playerCount,
        variant = config.variant,
    )

    fun getRoomStateData(): RoomStateData = RoomStateData(
        roomId = config.roomId,
        roomName = config.roomName,
        maxPlayers = config.maxPlayers,
        smallBlind = config.smallBlind,
        bigBlind = config.bigBlind,
        minBuyIn = config.minBuyIn,
        maxBuyIn = config.maxBuyIn,
        variant = config.variant,
        gameState = game.currentState,
        minDenomination = config.minDenomination,
    )
}
