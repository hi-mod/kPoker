package com.aaronchancey.poker.kpoker.player

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.DealtCard
import kotlinx.serialization.Serializable

typealias PlayerId = String
typealias ChipAmount = Double

@Serializable
data class Player(
    val id: PlayerId,
    val name: String,
    val avatarUrl: String? = null,
)

@Serializable
enum class PlayerStatus {
    WAITING, // Waiting for next hand
    ACTIVE, // In current hand
    FOLDED, // Folded this hand
    ALL_IN, // All-in this hand
    SITTING_OUT, // Temporarily sitting out
    DISCONNECTED, // Lost connection
}

/**
 * Tracks a player's showdown status during the reveal phase.
 */
@Serializable
enum class ShowdownStatus {
    /** Player hasn't acted yet in showdown */
    PENDING,

    /** Player chose to show their cards */
    SHOWN,

    /** Player chose to muck their cards */
    MUCKED,
}

@Serializable
data class PlayerState(
    val player: Player,
    val chips: ChipAmount,
    val holeCards: List<Card> = emptyList(),
    val dealtCards: List<DealtCard> = emptyList(),
    val status: PlayerStatus = PlayerStatus.WAITING,
    val currentBet: ChipAmount = 0.0,
    val totalBetThisRound: ChipAmount = 0.0,
    val isDealer: Boolean = false,
    val isSmallBlind: Boolean = false,
    val isBigBlind: Boolean = false,
    val hasActed: Boolean = false,
    val timeBank: Int = 30, // seconds
    val showdownStatus: ShowdownStatus? = null,
) {
    val isActive: Boolean get() = status == PlayerStatus.ACTIVE
    val canAct: Boolean get() = status == PlayerStatus.ACTIVE && !hasActed

    fun withChips(amount: ChipAmount) = copy(chips = amount)
    fun withStatus(newStatus: PlayerStatus) = copy(status = newStatus)
    fun withBet(amount: ChipAmount) = copy(currentBet = amount, totalBetThisRound = totalBetThisRound + amount)
    fun withHoleCards(cards: List<com.aaronchancey.poker.kpoker.core.Card>) = copy(holeCards = cards)

    /**
     * Sets the dealt cards with visibility information.
     * Also keeps holeCards in sync by extracting non-null card values.
     */
    fun withDealtCards(dealt: List<DealtCard>) = copy(
        dealtCards = dealt,
        holeCards = dealt.mapNotNull { it.card },
    )

    fun clearBet() = copy(currentBet = 0.0)
    fun markActed() = copy(hasActed = true)
    fun resetForNewRound() = copy(hasActed = false, currentBet = 0.0, totalBetThisRound = 0.0)
    fun withShowdownStatus(status: ShowdownStatus) = copy(showdownStatus = status)
    fun resetForNewHand() = copy(
        holeCards = emptyList(),
        dealtCards = emptyList(),
        status = PlayerStatus.WAITING,
        currentBet = 0.0,
        totalBetThisRound = 0.0,
        hasActed = false,
        isDealer = false,
        isSmallBlind = false,
        isBigBlind = false,
        showdownStatus = null,
    )
}
