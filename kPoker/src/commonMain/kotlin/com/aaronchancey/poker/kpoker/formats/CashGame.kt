package com.aaronchancey.poker.kpoker.formats

import com.aaronchancey.poker.kpoker.events.GameEvent
import com.aaronchancey.poker.kpoker.game.PokerGame
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.kpoker.room.Room

class CashGame(
    val config: CashGameConfig,
    val room: Room,
    private val game: PokerGame,
) {
    private var isRunning = false
    private var handsPlayed = 0L

    val currentState get() = game.currentState
    val handsCount get() = handsPlayed

    init {
        game.addEventListener { event ->
            when (event) {
                is GameEvent.HandComplete -> {
                    handsPlayed++
                    applyRake(event.winners)
                }

                else -> {}
            }
        }
    }

    fun start() {
        require(room.playerCount >= config.minPlayers) {
            "Need at least ${config.minPlayers} players to start"
        }
        isRunning = true
        game.initialize(room.currentTable)
    }

    fun stop() {
        isRunning = false
    }

    fun startNextHand() {
        require(isRunning) { "Game is not running" }
        require(room.playerCount >= config.minPlayers) {
            "Need at least ${config.minPlayers} players"
        }

        game.initialize(room.currentTable)
        game.startHand()
    }

    fun buyIn(playerId: PlayerId, amount: ChipAmount): Result<Unit> {
        if (amount < config.minBuyIn || amount > config.maxBuyIn) {
            return Result.failure(
                IllegalArgumentException(
                    "Buy-in must be between ${config.minBuyIn} and ${config.maxBuyIn}",
                ),
            )
        }

        // Room handles the actual seating, this just validates
        return Result.success(Unit)
    }

    fun rebuy(playerId: PlayerId, amount: ChipAmount): Result<Unit> {
        if (!config.allowRebuy) {
            return Result.failure(IllegalStateException("Rebuys not allowed"))
        }

        val playerState = room.currentTable.getPlayerSeat(playerId)?.playerState
            ?: return Result.failure(IllegalStateException("Player not seated"))

        if (playerState.chips > 0) {
            return Result.failure(IllegalStateException("Can only rebuy when chips are 0"))
        }

        if (amount < config.minBuyIn || amount > config.maxBuyIn) {
            return Result.failure(IllegalArgumentException("Invalid rebuy amount"))
        }

        // Update player chips - would need to update table in room
        return Result.success(Unit)
    }

    fun topUp(playerId: PlayerId, amount: ChipAmount): Result<Unit> {
        if (!config.allowTopUp) {
            return Result.failure(IllegalStateException("Top-ups not allowed"))
        }

        val playerState = room.currentTable.getPlayerSeat(playerId)?.playerState
            ?: return Result.failure(IllegalStateException("Player not seated"))

        val newTotal = playerState.chips + amount
        if (newTotal > config.maxBuyIn) {
            return Result.failure(
                IllegalArgumentException(
                    "Top-up would exceed max buy-in of ${config.maxBuyIn}",
                ),
            )
        }

        return Result.success(Unit)
    }

    fun cashOut(playerId: PlayerId): Result<ChipAmount> = room.standUp(playerId)

    private fun applyRake(winners: List<com.aaronchancey.poker.kpoker.game.Winner>) {
        if (config.rakePercent <= 0) return

        // Calculate and deduct rake from winnings
        // This would typically be done before awarding to players
    }

    companion object {
        fun create(
            config: CashGameConfig,
            room: Room,
            game: PokerGame,
        ): CashGame = CashGame(config, room, game)
    }
}
