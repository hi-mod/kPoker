package com.poker.common.domain

import java.time.Instant
import java.util.Date

data class Game(
    val name: String = "",
    val description: String = "",
    val inProgress: Boolean = false,
    val gameType: GameType = GameType.RingGame,
    val tableNumber: UInt = 1u,
    val level: Level = Level(),
    val id: String = "",
    val deck: Deck = Deck(),
    val started: Date = Date.from(Instant.now()),
    val buttonPosition: Int = 0,
    val players: List<Player> = emptyList(),
    val handNumber: Long = 0,
    val activePlayer: Player? = null,
    val board: List<Card> = emptyList(),
    val smallestChipSizeInPlay: Double = 0.01,
    val pot: Double = 0.0,
    val viewers: List<String> = emptyList(),
    val maxPlayers: Short = 10,
    val minPlayers: Short = 2,
) {
    fun pot() = pot + players.sumOf { it.currentWager }

    fun addPlayer(player: Player) = copy(players = players.plus(player))
    fun removePlayer(player: Player) = copy(players = players.minus(player))

    fun addViewer(userId: String) = copy(viewers = viewers.plus(userId))

    private fun calculateAndPayWinners(): List<Player> {
        val winningPlayers = winningPlayers()
        val chipsPerPlayer = pot.toInt() / winningPlayers.size
        var remainingChips = pot.toInt() % winningPlayers.size

        return winningPlayers.map { player ->
            var updatedPlayer = player.wins(chipsPerPlayer)
            if (remainingChips > 0) {
                updatedPlayer = updatedPlayer.wins(1)
                remainingChips--
            }
            updatedPlayer
        }
    }

    private fun winningPlayers(): List<Player> {
        val winningPlayers = players
            .filter { !it.hasFolded }
            .map { player ->
                val hand = player.hand.plus(board).sortedByDescending { it.rank }
                Pair(player, player.handRank(player.extractBestFiveCardHand(player.handKind(hand), hand)))
            }
            .sortedByDescending { (_, rank) -> rank }

        return winningPlayers.filter { (_, rank) -> rank == winningPlayers.first().second }.map { it.first }
    }

    private fun currentWager() = players.maxOf { it.currentWager }

    private fun onlyOnePlayerLeft() = players.size - players.count { it.hasFolded } == 1

    fun allPlayersHaveActed() =
        players.filter { !it.hasFolded }
            .all { it.currentWager == currentWager() && it.hasActed }

    fun dealCards(): Game {
        val firstRoundPlayers = players.map { player ->
            player.copy(hand = player.hand + deck.popCard())
        }
        val secondRoundPlayers = firstRoundPlayers.map { player ->
            player.copy(hand = player.hand + deck.popCard())
        }
        return copy(players = secondRoundPlayers)
    }

    fun performPlayerAction(gameEvent: GameEvent.SelectPlayerAction) = when (gameEvent) {
        is GameEvent.SelectPlayerAction.Bet, is GameEvent.SelectPlayerAction.Raise -> this.copy(
            players = players.subList(1, players.size) + players.first().wager(gameEvent.amount),
            pot = pot + gameEvent.amount,
        )
        is GameEvent.SelectPlayerAction.Call -> this.copy(
            players = players.subList(1, players.size) + players.first().wager(currentWager()),
            pot = pot + currentWager()
        )
        GameEvent.SelectPlayerAction.Check -> this.copy(
            players = players.subList(1, players.size) + players.first().check()
        )
        GameEvent.SelectPlayerAction.Fold -> this.copy(
            players = players.subList(1, players.size) + players.first().fold()
        )
    }

    fun awardPot(): Game {
        val winningPlayers = if (onlyOnePlayerLeft()) {
            players.filter { !it.hasFolded }.map { it.wins(pot) }
        } else {
            calculateAndPayWinners()
        }
        return copy(
            players = players.map { player ->
                winningPlayers.firstOrNull { winningPlayer -> winningPlayer.id == player.id }
                    ?: player.copy(currentWager = 0.0)
            },
        )
    }

    fun endHand() = copy(
        board = emptyList(),
        players = players.map { player ->
            player.copy(
                hand = emptyList(),
                currentWager = 0.0,
                hasActed = false,
                hasFolded = false,
            )
        },
    )
}
