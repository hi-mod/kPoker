package com.aaronchancey.poker.kpoker.formats

import com.aaronchancey.poker.kpoker.events.GameEvent
import com.aaronchancey.poker.kpoker.game.PokerGame
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.kpoker.player.Table

/**
 * Cash game format wrapper for PokerGame.
 *
 * Handles cash game specific logic:
 * - Buy-in/rebuy/top-up validation
 * - Rake calculation
 * - Hand counting
 *
 * Note: Seating/standing players is handled by the server layer (ServerRoom).
 * This class only validates buy-in amounts and applies rake.
 */
class CashGame(
    val config: CashGameConfig,
    private val game: PokerGame,
) {
    private var isRunning = false
    private var handsPlayed = 0L

    val currentState get() = game.currentState
    val handsCount get() = handsPlayed
    val table get() = game.currentState.table

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

    /**
     * Start the cash game with the given table.
     */
    fun start(table: Table) {
        require(table.playerCount >= config.minPlayers) {
            "Need at least ${config.minPlayers} players to start"
        }
        isRunning = true
        game.initialize(table)
    }

    fun stop() {
        isRunning = false
    }

    /**
     * Update the table (e.g., after a player sits/stands).
     */
    fun updateTable(table: Table) {
        game.updateTable(table)
    }

    /**
     * Start the next hand if conditions are met.
     */
    fun startNextHand() {
        require(isRunning) { "Game is not running" }
        require(table.playerCount >= config.minPlayers) {
            "Need at least ${config.minPlayers} players"
        }
        game.startHand()
    }

    /**
     * Validate a buy-in amount.
     * @return Success if valid, Failure with reason if not.
     */
    fun validateBuyIn(amount: ChipAmount): Result<ChipAmount> = when {
        amount < config.minBuyIn -> Result.failure(
            IllegalArgumentException("Buy-in below minimum: ${config.minBuyIn}"),
        )

        amount > config.maxBuyIn -> Result.success(config.maxBuyIn)

        // Cap at max
        else -> Result.success(amount)
    }

    /**
     * Validate a rebuy request.
     */
    fun validateRebuy(playerId: PlayerId, amount: ChipAmount): Result<Unit> {
        if (!config.allowRebuy) {
            return Result.failure(IllegalStateException("Rebuys not allowed"))
        }

        val playerState = table.getPlayerSeat(playerId)?.playerState
            ?: return Result.failure(IllegalStateException("Player not seated"))

        if (playerState.chips > 0) {
            return Result.failure(IllegalStateException("Can only rebuy when chips are 0"))
        }

        if (amount < config.minBuyIn || amount > config.maxBuyIn) {
            return Result.failure(IllegalArgumentException("Invalid rebuy amount"))
        }

        return Result.success(Unit)
    }

    /**
     * Validate a top-up request.
     */
    fun validateTopUp(playerId: PlayerId, amount: ChipAmount): Result<Unit> {
        if (!config.allowTopUp) {
            return Result.failure(IllegalStateException("Top-ups not allowed"))
        }

        val playerState = table.getPlayerSeat(playerId)?.playerState
            ?: return Result.failure(IllegalStateException("Player not seated"))

        val newTotal = playerState.chips + amount
        if (newTotal > config.maxBuyIn) {
            return Result.failure(
                IllegalArgumentException("Top-up would exceed max buy-in of ${config.maxBuyIn}"),
            )
        }

        return Result.success(Unit)
    }

    private fun applyRake(winners: List<com.aaronchancey.poker.kpoker.game.Winner>) {
        if (config.rakePercent <= 0) return
        // TODO: Calculate and deduct rake from winnings
        // This would typically be done before awarding to players
    }

    companion object {
        fun create(config: CashGameConfig, game: PokerGame): CashGame = CashGame(config, game)
    }
}
