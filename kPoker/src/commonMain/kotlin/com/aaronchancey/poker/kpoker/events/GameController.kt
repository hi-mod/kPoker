package com.aaronchancey.poker.kpoker.events

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.ActionRequest
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId

interface GameController {
    val currentState: GameState

    // Game flow
    fun startHand()
    fun processAction(action: Action): Result<GameState>
    fun getActionRequest(playerId: PlayerId): ActionRequest?

    // Event handling
    fun addEventListener(listener: (GameEvent) -> Unit)
    fun removeEventListener(listener: (GameEvent) -> Unit)

    // State queries
    fun getVisibleState(playerId: PlayerId): GameState
    fun getSpectatorState(): GameState
}

interface RoomController {
    // Room management
    fun joinRoom(playerId: PlayerId, playerName: String)
    fun leaveRoom(playerId: PlayerId)

    // Seating
    fun requestSeat(playerId: PlayerId, seatNumber: Int, buyIn: ChipAmount): Result<Unit>
    fun standUp(playerId: PlayerId): Result<ChipAmount>
    fun reserveSeat(playerId: PlayerId, seatNumber: Int, durationSeconds: Int): Result<Unit>

    // Spectating
    fun joinAsSpectator(playerId: PlayerId, playerName: String)
    fun leaveSpectating(playerId: PlayerId)
}
