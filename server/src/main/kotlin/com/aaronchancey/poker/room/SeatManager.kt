package com.aaronchancey.poker.room

import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.kpoker.player.Table

/**
 * Manages seat reservations for a poker room.
 *
 * Reservations are server-side network concerns - they hold a seat while
 * a player confirms their buy-in amount over a potentially slow connection.
 * Reservations expire after a configurable duration (default 60 seconds).
 */

data class SeatReservation(
    val seatNumber: Int,
    val playerId: PlayerId,
    val expiresAt: Long,
) {
    fun isExpired(currentTime: Long): Boolean = currentTime >= expiresAt
}

sealed class SeatSelectionResult {
    data class Success(val seatNumber: Int) : SeatSelectionResult()
    data class SeatOccupied(val seatNumber: Int) : SeatSelectionResult()
    data class SeatReserved(val seatNumber: Int, val expiresIn: Long) : SeatSelectionResult()
    data class InsufficientBuyIn(val required: ChipAmount, val provided: ChipAmount) : SeatSelectionResult()
    data class AlreadySeated(val currentSeat: Int) : SeatSelectionResult()
    data class TableFull(val maxPlayers: Int) : SeatSelectionResult()
    data class InvalidSeat(val seatNumber: Int, val maxSeat: Int) : SeatSelectionResult()
    data class PlayerNotInRoom(val playerId: PlayerId) : SeatSelectionResult()
}

data class SeatInfo(
    val number: Int,
    val status: SeatStatus,
    val playerName: String? = null,
    val chips: ChipAmount? = null,
    val reservedBy: PlayerId? = null,
    val reservationExpiresIn: Long? = null,
)

enum class SeatStatus {
    EMPTY,
    OCCUPIED,
    RESERVED,
}

class SeatManager(
    val maxSeats: Int,
    private val reservationDurationMs: Long = 60_000, // 1 minute default
) {
    private val reservations = mutableMapOf<Int, SeatReservation>()

    fun getAvailableSeats(table: Table, currentTime: Long): List<Int> {
        cleanExpiredReservations(currentTime)
        return (1..maxSeats).filter { seatNumber ->
            table.getSeat(seatNumber)?.isEmpty == true &&
                !reservations.containsKey(seatNumber)
        }
    }

    fun getSeatInfo(table: Table, currentTime: Long): List<SeatInfo> {
        cleanExpiredReservations(currentTime)
        return (1..maxSeats).map { seatNumber ->
            val seat = table.getSeat(seatNumber)
            val reservation = reservations[seatNumber]

            when {
                seat?.isOccupied == true -> SeatInfo(
                    number = seatNumber,
                    status = SeatStatus.OCCUPIED,
                    playerName = seat.playerState?.player?.name,
                    chips = seat.playerState?.chips,
                )

                reservation != null -> SeatInfo(
                    number = seatNumber,
                    status = SeatStatus.RESERVED,
                    reservedBy = reservation.playerId,
                    reservationExpiresIn = reservation.expiresAt - currentTime,
                )

                else -> SeatInfo(
                    number = seatNumber,
                    status = SeatStatus.EMPTY,
                )
            }
        }
    }

    fun reserveSeat(
        seatNumber: Int,
        playerId: PlayerId,
        table: Table,
        currentTime: Long,
        durationMs: Long = reservationDurationMs,
    ): SeatSelectionResult {
        cleanExpiredReservations(currentTime)

        if (seatNumber !in 1..maxSeats) {
            return SeatSelectionResult.InvalidSeat(seatNumber, maxSeats)
        }

        val seat = table.getSeat(seatNumber) ?: return SeatSelectionResult.InvalidSeat(seatNumber, maxSeats)

        if (seat.isOccupied) {
            return SeatSelectionResult.SeatOccupied(seatNumber)
        }

        val existingReservation = reservations[seatNumber]
        if (existingReservation != null && existingReservation.playerId != playerId) {
            return SeatSelectionResult.SeatReserved(
                seatNumber,
                existingReservation.expiresAt - currentTime,
            )
        }

        reservations[seatNumber] = SeatReservation(
            seatNumber = seatNumber,
            playerId = playerId,
            expiresAt = currentTime + durationMs,
        )

        return SeatSelectionResult.Success(seatNumber)
    }

    fun cancelReservation(playerId: PlayerId) {
        val iterator = reservations.entries.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().value.playerId == playerId) {
                iterator.remove()
            }
        }
    }

    fun hasReservation(seatNumber: Int, playerId: PlayerId, currentTime: Long): Boolean {
        cleanExpiredReservations(currentTime)
        return reservations[seatNumber]?.playerId == playerId
    }

    fun consumeReservation(seatNumber: Int, playerId: PlayerId): Boolean {
        val reservation = reservations[seatNumber]
        return if (reservation?.playerId == playerId) {
            reservations.remove(seatNumber)
            true
        } else {
            false
        }
    }

    private fun cleanExpiredReservations(currentTime: Long) {
        val iterator = reservations.entries.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().value.isExpired(currentTime)) {
                iterator.remove()
            }
        }
    }
}
