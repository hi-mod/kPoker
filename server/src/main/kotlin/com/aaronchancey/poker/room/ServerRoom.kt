package com.aaronchancey.poker.room

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.BettingStructure
import com.aaronchancey.poker.kpoker.game.GamePhase
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.Seat
import com.aaronchancey.poker.kpoker.player.Table
import com.aaronchancey.poker.kpoker.variants.TexasHoldemGame
import com.aaronchancey.poker.message.RoomInfo
import com.aaronchancey.poker.message.ServerMessage
import com.aaronchancey.poker.ws.ConnectionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ServerRoom(
    val roomId: String,
    val roomName: String,
    val maxPlayers: Int = 9,
    val smallBlind: ChipAmount = 1,
    val bigBlind: ChipAmount = 2,
    val minBuyIn: ChipAmount = 40,
    val maxBuyIn: ChipAmount = 200,
    private val connectionManager: ConnectionManager,
) {
    private val mutex = Mutex()
    private val bettingStructure = BettingStructure.noLimit(smallBlind, bigBlind)
    private var table = Table.create(roomId, roomName, maxPlayers)
    private var game = TexasHoldemGame(bettingStructure)
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        game.initialize(table)
        game.addEventListener { event ->
            scope.launch {
                connectionManager.broadcast(roomId, ServerMessage.GameEventOccurred(event))
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
    )

    fun getGameState(): GameState = game.currentState

    suspend fun seatPlayer(playerId: PlayerId, playerName: String, seatNumber: Int, buyIn: ChipAmount): Result<Unit> = mutex.withLock {
        try {
            val player = Player(playerId, playerName)
            val playerState = PlayerState(player, buyIn)
            table = table.sitPlayer(seatNumber, playerState)
            game.initialize(table)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun standPlayer(playerId: PlayerId): Result<ChipAmount> {
        return mutex.withLock {
            try {
                val seat = table.getPlayerSeat(playerId)
                    ?: return@withLock Result.failure(IllegalStateException("Player not seated"))
                val chips = seat.playerState?.chips ?: 0
                table = table.standPlayer(playerId)
                game.initialize(table)
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
        val state = game.currentState
        if (state.table.playerCount >= 2 && state.phase == GamePhase.WAITING) {
            game.startHand()
            true
        } else {
            false
        }
    }

    fun isPlayerSeated(playerId: PlayerId): Boolean = game.currentState.table.getPlayerSeat(playerId) != null

    fun getPlayerSeat(playerId: PlayerId): Seat? = game.currentState.table.getPlayerSeat(playerId)
}
