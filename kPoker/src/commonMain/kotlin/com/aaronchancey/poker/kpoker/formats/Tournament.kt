package com.aaronchancey.poker.kpoker.formats

import com.aaronchancey.poker.kpoker.betting.BettingStructure
import com.aaronchancey.poker.kpoker.game.PokerGame
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.Player
import com.aaronchancey.poker.kpoker.player.PlayerId

enum class TournamentStatus {
    REGISTERING,
    STARTING,
    RUNNING,
    PAUSED,
    FINAL_TABLE,
    HEADS_UP,
    COMPLETE,
    CANCELLED,
}

data class TournamentPlayer(
    val player: Player,
    var chips: ChipAmount,
    val buyInTime: Long,
    var finishPosition: Int? = null,
    var finishTime: Long? = null,
    var winnings: ChipAmount = 0,
    var rebuys: Int = 0,
    var addon: Boolean = false,
)

data class TournamentState(
    val status: TournamentStatus,
    val currentLevel: Int,
    val levelStartTime: Long,
    val players: Map<PlayerId, TournamentPlayer>,
    val eliminationOrder: List<PlayerId> = emptyList(),
    val handsPlayed: Long = 0,
) {
    val activePlayers: List<TournamentPlayer>
        get() = players.values.filter { it.finishPosition == null }

    val averageStack: ChipAmount
        get() = if (activePlayers.isEmpty()) {
            0
        } else {
            activePlayers.sumOf { it.chips } / activePlayers.size
        }

    val currentBlindLevel: BlindLevel? = null // Set from config
}

sealed class TournamentEvent {
    data class PlayerRegistered(val player: Player, val timestamp: Long) : TournamentEvent()
    data class PlayerEliminated(val player: Player, val position: Int, val winnings: ChipAmount) : TournamentEvent()
    data class LevelChanged(val newLevel: BlindLevel) : TournamentEvent()
    data class TournamentStarted(val playerCount: Int) : TournamentEvent()
    data class TournamentComplete(val winner: Player, val results: List<TournamentPlayer>) : TournamentEvent()
    data class BreakStarted(val durationMinutes: Int) : TournamentEvent()
    data class FinalTableReached(val players: List<TournamentPlayer>) : TournamentEvent()
}

class Tournament(
    val config: TournamentConfig,
    private val gameFactory: (BettingStructure) -> PokerGame,
) {
    private var state = TournamentState(
        status = TournamentStatus.REGISTERING,
        currentLevel = 0,
        levelStartTime = 0,
        players = emptyMap(),
    )

    private var currentGame: PokerGame? = null
    private val eventListeners = mutableListOf<(TournamentEvent) -> Unit>()

    val currentState: TournamentState get() = state
    val isRunning: Boolean get() = state.status == TournamentStatus.RUNNING

    fun addEventListener(listener: (TournamentEvent) -> Unit) {
        eventListeners.add(listener)
    }

    private fun emit(event: TournamentEvent) {
        eventListeners.forEach { it(event) }
    }

    // Registration
    fun register(player: Player, currentTime: Long): Result<Unit> {
        if (state.status != TournamentStatus.REGISTERING) {
            // Check late registration
            if (state.status == TournamentStatus.RUNNING &&
                state.currentLevel <= config.lateRegistrationLevels
            ) {
                return lateRegister(player, currentTime)
            }
            return Result.failure(IllegalStateException("Registration closed"))
        }

        if (state.players.size >= config.maxPlayers) {
            return Result.failure(IllegalStateException("Tournament full"))
        }

        if (state.players.containsKey(player.id)) {
            return Result.failure(IllegalStateException("Already registered"))
        }

        val tournamentPlayer = TournamentPlayer(
            player = player,
            chips = config.startingChips,
            buyInTime = currentTime,
        )

        state = state.copy(
            players = state.players + (player.id to tournamentPlayer),
        )

        emit(TournamentEvent.PlayerRegistered(player, currentTime))
        return Result.success(Unit)
    }

    private fun lateRegister(player: Player, currentTime: Long): Result<Unit> {
        val tournamentPlayer = TournamentPlayer(
            player = player,
            chips = config.startingChips,
            buyInTime = currentTime,
        )

        state = state.copy(
            players = state.players + (player.id to tournamentPlayer),
        )

        emit(TournamentEvent.PlayerRegistered(player, currentTime))
        return Result.success(Unit)
    }

    fun unregister(playerId: PlayerId): Result<ChipAmount> {
        if (state.status != TournamentStatus.REGISTERING) {
            return Result.failure(IllegalStateException("Cannot unregister after tournament starts"))
        }

        if (!state.players.containsKey(playerId)) {
            return Result.failure(IllegalStateException("Not registered"))
        }

        state = state.copy(players = state.players - playerId)
        return Result.success(config.buyIn) // Refund buy-in
    }

    // Tournament flow
    fun start(currentTime: Long): Result<Unit> {
        if (state.players.size < config.minPlayers) {
            return Result.failure(
                IllegalStateException(
                    "Need at least ${config.minPlayers} players",
                ),
            )
        }

        val firstLevel = config.blindLevels.first()
        state = state.copy(
            status = TournamentStatus.RUNNING,
            currentLevel = 1,
            levelStartTime = currentTime,
        )

        // Create game with current blind level
        val bettingStructure = BettingStructure.noLimit(
            firstLevel.smallBlind,
            firstLevel.bigBlind,
            firstLevel.ante,
        )
        currentGame = gameFactory(bettingStructure)

        emit(TournamentEvent.TournamentStarted(state.players.size))
        emit(TournamentEvent.LevelChanged(firstLevel))

        return Result.success(Unit)
    }

    fun advanceLevel(currentTime: Long) {
        val nextLevelIndex = state.currentLevel
        if (nextLevelIndex >= config.blindLevels.size) return

        val nextLevel = config.blindLevels[nextLevelIndex]
        state = state.copy(
            currentLevel = state.currentLevel + 1,
            levelStartTime = currentTime,
        )

        // Update game betting structure
        val bettingStructure = BettingStructure.noLimit(
            nextLevel.smallBlind,
            nextLevel.bigBlind,
            nextLevel.ante,
        )
        currentGame = gameFactory(bettingStructure)

        emit(TournamentEvent.LevelChanged(nextLevel))
    }

    fun eliminatePlayer(playerId: PlayerId, currentTime: Long) {
        val player = state.players[playerId] ?: return

        val position = state.activePlayers.size
        val payout = calculatePayout(position)

        val updatedPlayer = player.copy(
            finishPosition = position,
            finishTime = currentTime,
            winnings = payout,
        )

        state = state.copy(
            players = state.players + (playerId to updatedPlayer),
            eliminationOrder = state.eliminationOrder + playerId,
        )

        emit(TournamentEvent.PlayerEliminated(player.player, position, payout))

        // Check for tournament end
        checkTournamentEnd(currentTime)
    }

    private fun calculatePayout(position: Int): ChipAmount {
        val payoutEntry = config.payoutStructure.find { it.place == position }
            ?: return 0

        val totalPrizePool = config.buyIn * state.players.size
        return (totalPrizePool * payoutEntry.percentOfPrizePool / 100).toLong()
    }

    private fun checkTournamentEnd(currentTime: Long) {
        when (state.activePlayers.size) {
            1 -> {
                // Tournament complete
                val winner = state.activePlayers.first()
                val winnerPayout = calculatePayout(1)

                val updatedWinner = winner.copy(
                    finishPosition = 1,
                    finishTime = currentTime,
                    winnings = winnerPayout,
                )

                state = state.copy(
                    status = TournamentStatus.COMPLETE,
                    players = state.players + (winner.player.id to updatedWinner),
                )

                emit(
                    TournamentEvent.TournamentComplete(
                        winner.player,
                        state.players.values.toList(),
                    ),
                )
            }

            2 -> {
                state = state.copy(status = TournamentStatus.HEADS_UP)
            }

            config.maxPlayers.coerceAtMost(10) -> {
                if (config.tablesCount > 1) {
                    state = state.copy(status = TournamentStatus.FINAL_TABLE)
                    emit(TournamentEvent.FinalTableReached(state.activePlayers))
                }
            }
        }
    }

    // Rebuy/Addon
    fun rebuy(playerId: PlayerId): Result<Unit> {
        if (!config.rebuyAllowed) {
            return Result.failure(IllegalStateException("Rebuys not allowed"))
        }

        if (state.currentLevel > config.rebuyLevels) {
            return Result.failure(IllegalStateException("Rebuy period ended"))
        }

        val player = state.players[playerId]
            ?: return Result.failure(IllegalStateException("Player not in tournament"))

        if (player.chips > 0) {
            return Result.failure(IllegalStateException("Can only rebuy when eliminated"))
        }

        val updatedPlayer = player.copy(
            chips = config.rebuyChips,
            rebuys = player.rebuys + 1,
        )

        state = state.copy(
            players = state.players + (playerId to updatedPlayer),
        )

        return Result.success(Unit)
    }

    fun addon(playerId: PlayerId): Result<Unit> {
        if (!config.addonAllowed) {
            return Result.failure(IllegalStateException("Addons not allowed"))
        }

        val player = state.players[playerId]
            ?: return Result.failure(IllegalStateException("Player not in tournament"))

        if (player.addon) {
            return Result.failure(IllegalStateException("Already took addon"))
        }

        val updatedPlayer = player.copy(
            chips = player.chips + config.addonChips,
            addon = true,
        )

        state = state.copy(
            players = state.players + (playerId to updatedPlayer),
        )

        return Result.success(Unit)
    }

    fun getResults(): List<TournamentPlayer> = state.players.values
        .sortedBy { it.finishPosition ?: Int.MAX_VALUE }

    fun getCurrentBlindLevel(): BlindLevel? = config.blindLevels.getOrNull(state.currentLevel - 1)

    fun getTimeUntilNextLevel(currentTime: Long): Long {
        val currentLevel = getCurrentBlindLevel() ?: return 0
        val levelDurationMs = currentLevel.durationMinutes * 60 * 1000L
        val elapsed = currentTime - state.levelStartTime
        return maxOf(0, levelDurationMs - elapsed)
    }
}
