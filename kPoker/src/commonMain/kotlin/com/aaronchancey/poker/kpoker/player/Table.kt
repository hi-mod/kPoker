package com.aaronchancey.poker.kpoker.player

data class Seat(
    val number: Int,
    val playerState: PlayerState? = null,
) {
    val isEmpty: Boolean get() = playerState == null
    val isOccupied: Boolean get() = playerState != null
    val player: Player? get() = playerState?.player

    fun sitPlayer(state: PlayerState): Seat = copy(playerState = state)
    fun standPlayer(): Seat = copy(playerState = null)
    fun updatePlayerState(update: (PlayerState) -> PlayerState): Seat = copy(playerState = playerState?.let(update))
}

data class Table(
    val id: String,
    val name: String,
    val seats: List<Seat>,
    val maxPlayers: Int = seats.size,
) {
    init {
        require(seats.size == maxPlayers) { "Seats count must match maxPlayers" }
        require(seats.map { it.number }.distinct().size == seats.size) { "Seat numbers must be unique" }
    }

    val occupiedSeats: List<Seat> get() = seats.filter { it.isOccupied }
    val emptySeats: List<Seat> get() = seats.filter { it.isEmpty }
    val playerCount: Int get() = occupiedSeats.size
    val isFull: Boolean get() = emptySeats.isEmpty()

    fun getSeat(number: Int): Seat? = seats.find { it.number == number }

    fun getPlayerSeat(playerId: PlayerId): Seat? = seats.find { it.playerState?.player?.id == playerId }

    fun sitPlayer(seatNumber: Int, playerState: PlayerState): Table {
        val seat = getSeat(seatNumber)
        require(seat != null) { "Invalid seat number: $seatNumber" }
        require(seat.isEmpty) { "Seat $seatNumber is already occupied" }
        require(getPlayerSeat(playerState.player.id) == null) { "Player already seated at table" }

        return copy(
            seats = seats.map {
                if (it.number == seatNumber) it.sitPlayer(playerState) else it
            },
        )
    }

    fun standPlayer(playerId: PlayerId): Table = copy(
        seats = seats.map {
            if (it.playerState?.player?.id == playerId) it.standPlayer() else it
        },
    )

    fun updateSeat(seatNumber: Int, update: (Seat) -> Seat): Table = copy(
        seats = seats.map {
            if (it.number == seatNumber) update(it) else it
        },
    )

    fun updatePlayerState(playerId: PlayerId, update: (PlayerState) -> PlayerState): Table = copy(
        seats = seats.map { seat ->
            if (seat.playerState?.player?.id == playerId) {
                seat.updatePlayerState(update)
            } else {
                seat
            }
        },
    )

    fun getActivePlayers(): List<PlayerState> = occupiedSeats.mapNotNull { it.playerState }.filter { it.isActive }

    fun getPlayersInHand(): List<PlayerState> = occupiedSeats.mapNotNull { it.playerState }
        .filter { it.status in listOf(PlayerStatus.ACTIVE, PlayerStatus.ALL_IN) }

    companion object {
        fun create(id: String, name: String, maxPlayers: Int): Table {
            require(maxPlayers in 2..10) { "Table must have 2-10 seats" }
            val seats = (1..maxPlayers).map { Seat(number = it) }
            return Table(id, name, seats, maxPlayers)
        }
    }
}
