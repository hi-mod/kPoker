package com.aaronchancey.poker.kpoker.room

import com.aaronchancey.poker.kpoker.events.GameEvent
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.Table

data class RoomConfig(
    val id: String,
    val name: String,
    val maxPlayers: Int,
    val minBuyIn: ChipAmount,
    val maxBuyIn: ChipAmount,
    val maxSpectators: Int = 100,
    val allowSpectators: Boolean = true,
    val reservationDurationMs: Long = 60_000,
)

class Room(
    val config: RoomConfig,
    table: Table = Table.create(config.id, config.name, config.maxPlayers),
) {
    private val spectators = mutableMapOf<PlayerId, RoomParticipant.Spectator>()
    private val seatManager = SeatManager(config.maxPlayers, config.reservationDurationMs)
    private val eventListeners = mutableListOf<(GameEvent) -> Unit>()

    private var _currentTable = table
    val currentTable: Table
        get() = _currentTable

    val participants: List<RoomParticipant>
        get() = spectators.values.toList() +
            _currentTable.occupiedSeats.mapNotNull { seat ->
                seat.playerState?.let { ps ->
                    RoomParticipant.SeatedPlayer(ps.player, seat.number, ps.chips)
                }
            }

    val spectatorCount: Int get() = spectators.size
    val playerCount: Int get() = _currentTable.playerCount
    val isFull: Boolean get() = _currentTable.isFull

    fun addEventListener(listener: (GameEvent) -> Unit) {
        eventListeners.add(listener)
    }

    private fun emit(event: GameEvent) {
        eventListeners.forEach { it(event) }
    }

    // Room entry
    fun joinAsSpectator(player: Player): Result<Unit> {
        if (!config.allowSpectators) {
            return Result.failure(IllegalStateException("Spectators not allowed"))
        }
        if (spectators.size >= config.maxSpectators) {
            return Result.failure(IllegalStateException("Spectator limit reached"))
        }
        if (isPlayerInRoom(player.id)) {
            return Result.failure(IllegalStateException("Player already in room"))
        }

        spectators[player.id] = RoomParticipant.Spectator(player)
        emit(GameEvent.SpectatorJoined(player.id, player.name))
        return Result.success(Unit)
    }

    fun leaveRoom(playerId: PlayerId): Result<ChipAmount> {
        // Check if spectator
        if (spectators.remove(playerId) != null) {
            emit(GameEvent.SpectatorLeft(playerId))
            return Result.success(0L)
        }

        // Check if seated
        val seat = _currentTable.getPlayerSeat(playerId)
        if (seat != null) {
            val chips = seat.playerState?.chips ?: 0L
            _currentTable = _currentTable.standPlayer(playerId)
            seatManager.cancelReservation(playerId)
            emit(GameEvent.PlayerStoodUp(playerId, seat.number, chips))
            emit(GameEvent.PlayerLeftRoom(playerId))
            return Result.success(chips)
        }

        return Result.failure(IllegalStateException("Player not in room"))
    }

    // Seat management
    fun getAvailableSeats(currentTime: Long): List<Int> = seatManager.getAvailableSeats(_currentTable, currentTime)

    fun getSeatInfo(currentTime: Long): List<SeatInfo> = seatManager.getSeatInfo(_currentTable, currentTime)

    fun reserveSeat(playerId: PlayerId, seatNumber: Int, currentTime: Long): SeatSelectionResult {
        if (!isPlayerInRoom(playerId)) {
            return SeatSelectionResult.PlayerNotInRoom(playerId)
        }

        // Check if already seated
        val existingSeat = _currentTable.getPlayerSeat(playerId)
        if (existingSeat != null) {
            return SeatSelectionResult.AlreadySeated(existingSeat.number)
        }

        return seatManager.reserveSeat(seatNumber, playerId, _currentTable, currentTime)
    }

    fun takeSeat(
        player: Player,
        seatNumber: Int,
        buyIn: ChipAmount,
        currentTime: Long,
    ): SeatSelectionResult {
        // Validate seat number
        if (seatNumber < 1 || seatNumber > config.maxPlayers) {
            return SeatSelectionResult.InvalidSeat(seatNumber, config.maxPlayers)
        }

        // Check buy-in
        if (buyIn < config.minBuyIn) {
            return SeatSelectionResult.InsufficientBuyIn(config.minBuyIn, buyIn)
        }

        val actualBuyIn = minOf(buyIn, config.maxBuyIn)

        // Check if seat is available
        val seat = _currentTable.getSeat(seatNumber)
        if (seat?.isOccupied == true) {
            return SeatSelectionResult.SeatOccupied(seatNumber)
        }

        // Check reservation (either player has it or no reservation)
        val hasReservation = seatManager.hasReservation(seatNumber, player.id, currentTime)
        val reservations = seatManager.getSeatInfo(_currentTable, currentTime)
            .find { it.number == seatNumber }

        if (reservations?.status == SeatStatus.RESERVED && !hasReservation) {
            return SeatSelectionResult.SeatReserved(
                seatNumber,
                reservations.reservationExpiresIn ?: 0,
            )
        }

        // Check if already seated elsewhere
        val existingSeat = _currentTable.getPlayerSeat(player.id)
        if (existingSeat != null) {
            return SeatSelectionResult.AlreadySeated(existingSeat.number)
        }

        // Remove from spectators if present
        spectators.remove(player.id)

        // Consume reservation if exists
        seatManager.consumeReservation(seatNumber, player.id)

        // Sit player
        val playerState = PlayerState(player = player, chips = actualBuyIn)
        _currentTable = _currentTable.sitPlayer(seatNumber, playerState)

        emit(GameEvent.PlayerSeated(player.id, seatNumber, actualBuyIn))
        return SeatSelectionResult.Success(seatNumber)
    }

    fun standUp(playerId: PlayerId): Result<ChipAmount> {
        val seat = _currentTable.getPlayerSeat(playerId)
            ?: return Result.failure(IllegalStateException("Player not seated"))

        val chips = seat.playerState?.chips ?: 0L
        _currentTable = _currentTable.standPlayer(playerId)
        seatManager.cancelReservation(playerId)

        // Convert to spectator
        val player = seat.playerState?.player
        if (player != null && config.allowSpectators) {
            spectators[playerId] = RoomParticipant.Spectator(player)
        }

        emit(GameEvent.PlayerStoodUp(playerId, seat.number, chips))
        return Result.success(chips)
    }

    // Visibility
    fun getVisibleGameState(gameState: GameState, viewerId: PlayerId): GameState {
        val viewerSeat = _currentTable.getPlayerSeat(viewerId)
        val isSpectator = spectators.containsKey(viewerId)

        // Hide other players' hole cards
        val visibleTable = gameState.table.copy(
            seats = gameState.table.seats.map { seat ->
                if (seat.playerState == null) {
                    seat
                } else if (seat.playerState.player.id == viewerId) {
                    seat // Show own cards
                } else if (gameState.phase == com.aaronchancey.poker.kpoker.game.GamePhase.SHOWDOWN) {
                    seat // Show all cards at showdown
                } else {
                    seat.copy(
                        playerState = seat.playerState.copy(
                            holeCards = emptyList(), // Hide other players' cards
                        ),
                    )
                }
            },
        )

        return gameState.copy(table = visibleTable)
    }

    fun getSpectatorView(gameState: GameState): GameState {
        // Hide all hole cards for spectators (unless showdown)
        val visibleTable = gameState.table.copy(
            seats = gameState.table.seats.map { seat ->
                if (seat.playerState == null) {
                    seat
                } else if (gameState.phase == com.aaronchancey.poker.kpoker.game.GamePhase.SHOWDOWN) {
                    seat
                } else {
                    seat.copy(
                        playerState = seat.playerState.copy(holeCards = emptyList()),
                    )
                }
            },
        )

        return gameState.copy(table = visibleTable)
    }

    // Helpers
    fun isPlayerInRoom(playerId: PlayerId): Boolean = spectators.containsKey(playerId) || _currentTable.getPlayerSeat(playerId) != null

    fun isPlayerSeated(playerId: PlayerId): Boolean = _currentTable.getPlayerSeat(playerId) != null

    fun getPlayer(playerId: PlayerId): Player? = spectators[playerId]?.player
        ?: _currentTable.getPlayerSeat(playerId)?.playerState?.player

    fun updateTable(table: Table) {
        _currentTable = table
    }
}
