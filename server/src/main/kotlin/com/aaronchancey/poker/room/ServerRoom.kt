package com.aaronchancey.poker.room

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.ActionRequest
import com.aaronchancey.poker.kpoker.betting.BettingStructure
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

class ServerRoom(
    val roomId: String,
    val roomName: String,
    val minDenomination: ChipAmount = 0.1,
    val maxPlayers: Int = 9,
    val smallBlind: ChipAmount = 1.0,
    val bigBlind: ChipAmount = 2.0,
    val minBuyIn: ChipAmount = 40.0,
    val maxBuyIn: ChipAmount = 200.0,
    val variant: GameVariant = GameVariant.TEXAS_HOLDEM_NL,
    private val connectionManager: ConnectionManager,
    initialGameState: GameState? = null,
) {
    private val mutex = Mutex()
    private val bettingStructure: BettingStructure = when (variant) {
        GameVariant.OMAHA_PL, GameVariant.OMAHA_HILO_PL -> BettingStructure.potLimit(smallBlind, bigBlind, minDenomination = minDenomination)
        GameVariant.TEXAS_HOLDEM_NL -> BettingStructure.noLimit(smallBlind, bigBlind, minDenomination = minDenomination)
    }

    private var game: PokerGame = when (variant) {
        GameVariant.OMAHA_PL -> OmahaGame.potLimit(smallBlind, bigBlind, minDenomination = minDenomination)
        GameVariant.OMAHA_HILO_PL -> OmahaGame.potLimitHiLo(smallBlind, bigBlind, minDenomination = minDenomination)
        GameVariant.TEXAS_HOLDEM_NL -> TexasHoldemGame.noLimit(smallBlind, bigBlind, minDenomination = minDenomination)
    }

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        if (initialGameState != null) {
            game.restoreState(initialGameState)
        } else {
            val initialTable = Table.create(roomId, roomName, maxPlayers)
            game.initialize(initialTable)
        }

        game.addEventListener { event ->
            scope.launch {
                connectionManager.broadcast(roomId, ServerMessage.GameEventOccurred(event))
                broadcastVisibleGameState()

                when (event) {
                    is GameEvent.TurnChanged -> {
                        game.getActionRequest()?.let { actionRequest ->
                            connectionManager.sendTo(
                                roomId,
                                event.playerId,
                                ServerMessage.ActionRequired(actionRequest),
                            )
                        }
                    }

                    is GameEvent.HandComplete -> {
                        delay(5.seconds)
                        startHandIfReady()
                    }

                    else -> Unit /* No additional action needed for other events */
                }
            }
        }
    }

    fun getRoomInfo(): RoomInfo = RoomInfo(
        roomId = roomId,
        roomName = roomName,
        maxPlayers = maxPlayers,
        smallBlind = smallBlind,
        bigBlind = bigBlind,
        minBuyIn = minBuyIn,
        maxBuyIn = maxBuyIn,
        playerCount = game.currentState.table.playerCount,
        variant = variant,
    )

    fun getRoomStateData(): RoomStateData = RoomStateData(
        roomId = roomId,
        roomName = roomName,
        maxPlayers = maxPlayers,
        smallBlind = smallBlind,
        bigBlind = bigBlind,
        minBuyIn = minBuyIn,
        maxBuyIn = maxBuyIn,
        variant = variant,
        gameState = game.currentState,
        minDenomination = minDenomination,
    )

    fun getGameState(): GameState = game.currentState

    fun getActionRequest(): ActionRequest? = game.getActionRequest()

    suspend fun seatPlayer(playerId: PlayerId, playerName: String, seatNumber: Int, buyIn: ChipAmount): Result<Unit> = mutex.withLock {
        try {
            val player = Player(playerId, playerName)
            val playerState = PlayerState(player, buyIn)
            val currentTable = game.currentState.table
            val newTable = currentTable.sitPlayer(seatNumber, playerState)
            game.updateTable(newTable)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun standPlayer(playerId: PlayerId): Result<ChipAmount> {
        return mutex.withLock {
            try {
                val currentTable = game.currentState.table
                val seat = currentTable.getPlayerSeat(playerId)
                    ?: return@withLock Result.failure(IllegalStateException("Player not seated"))
                val chips = seat.playerState?.chips ?: 0.0
                val newTable = currentTable.standPlayer(playerId)
                game.updateTable(newTable)
                Result.success(chips)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun performAction(playerId: PlayerId, action: Action): Result<Unit> {
        return mutex.withLock {
            try {
                if (action.playerId != playerId) {
                    return@withLock Result.failure(IllegalArgumentException("Action playerId mismatch"))
                }
                game.processAction(action)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
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

    fun isPlayerSeated(playerId: PlayerId): Boolean = game.currentState.table.getPlayerSeat(playerId) != null

    fun getPlayerSeat(playerId: PlayerId): Seat? = game.currentState.table.getPlayerSeat(playerId)

    fun addGameEventListener(listener: (GameEvent) -> Unit) {
        game.addEventListener(listener)
    }

    suspend fun broadcastVisibleGameState() {
        val fullState = game.currentState
        connectionManager.getConnections(roomId).forEach { connection ->
            val visibleState = getVisibleGameState(fullState, connection.playerId)
            connectionManager.sendTo(roomId, connection.playerId, ServerMessage.GameStateUpdate(visibleState))
        }
    }

    private fun getVisibleGameState(gameState: GameState, viewerId: PlayerId): GameState {
        val visibleTable = gameState.table.copy(
            seats = gameState.table.seats.map { seat ->
                val playerState = seat.playerState
                when {
                    playerState == null -> seat

                    playerState.player.id == viewerId -> seat

                    gameState.phase == GamePhase.SHOWDOWN -> seat

                    else -> seat.copy(
                        playerState = playerState.copy(holeCards = emptyList()),
                    )
                }
            },
        )
        return gameState.copy(table = visibleTable)
    }
}
