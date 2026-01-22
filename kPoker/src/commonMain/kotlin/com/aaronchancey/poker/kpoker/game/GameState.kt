package com.aaronchancey.poker.kpoker.game

import com.aaronchancey.poker.kpoker.betting.Action
import com.aaronchancey.poker.kpoker.betting.BettingRound
import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.Deck
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.PotManager
import com.aaronchancey.poker.kpoker.player.Table
import kotlinx.serialization.Serializable

@Serializable
enum class GamePhase {
    WAITING, // Waiting for players
    STARTING, // Hand is starting
    POSTING_BLINDS, // Posting blinds/antes
    DEALING, // Dealing hole cards
    PRE_FLOP, // Pre-flop betting
    FLOP, // Flop betting
    TURN, // Turn betting
    RIVER, // River betting
    SHOWDOWN, // Determining winner
    HAND_COMPLETE, // Hand finished, ready for next
}

@Serializable
data class GameState(
    val table: Table,
    val variant: GameVariant = GameVariant.TEXAS_HOLDEM,
    val phase: GamePhase = GamePhase.WAITING,
    val deck: Deck = Deck.standard(),
    val communityCards: List<Card> = emptyList(),
    val potManager: PotManager = PotManager(),
    val bettingRound: BettingRound? = null,
    val dealerSeatNumber: Int = 1,
    val currentActorSeatNumber: Int? = null,
    val handNumber: Long = 0,
    val winners: List<Winner> = emptyList(),
    val lastAction: Action? = null,
    /** Last aggressor from final betting round - determines showdown order */
    val showdownAggressorId: PlayerId? = null,
) {
    val activePlayers: List<PlayerState> get() = table.getActivePlayers()
    val playersInHand: List<PlayerState> get() = table.getPlayersInHand()
    val totalPot: ChipAmount get() = potManager.totalPot

    val isHandInProgress: Boolean get() = phase !in listOf(GamePhase.WAITING, GamePhase.HAND_COMPLETE)

    val currentActor: PlayerState? get() =
        currentActorSeatNumber?.let { table.getSeat(it)?.playerState }

    fun withTable(table: Table) = copy(table = table)
    fun withPhase(phase: GamePhase) = copy(phase = phase)
    fun withCommunityCards(cards: List<Card>) = copy(communityCards = cards)
    fun addCommunityCards(cards: List<Card>) = copy(communityCards = communityCards + cards)
    fun withPotManager(pm: PotManager) = copy(potManager = pm)
    fun withBettingRound(round: BettingRound?) = copy(bettingRound = round)
    fun withCurrentActor(seatNumber: Int?) = copy(currentActorSeatNumber = seatNumber)
    fun withLastAction(action: Action) = copy(lastAction = action)
    fun withWinners(winners: List<Winner>) = copy(winners = winners)
    fun withShowdownAggressor(playerId: PlayerId?) = copy(showdownAggressorId = playerId)
    fun nextHand() = copy(
        phase = GamePhase.WAITING,
        communityCards = emptyList(),
        potManager = PotManager(),
        bettingRound = null,
        winners = emptyList(),
        lastAction = null,
        handNumber = handNumber + 1,
        showdownAggressorId = null,
    )
}

@Serializable
data class Winner(
    val playerId: PlayerId,
    val amount: ChipAmount,
    val handDescription: String,
    val potType: String = "main",
)
