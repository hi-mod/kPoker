package com.aaronchancey.poker.kpoker.player

typealias PlayerId = String
typealias ChipAmount = Long

data class Player(
    val id: PlayerId,
    val name: String,
    val avatarUrl: String? = null,
)

enum class PlayerStatus {
    WAITING, // Waiting for next hand
    ACTIVE, // In current hand
    FOLDED, // Folded this hand
    ALL_IN, // All-in this hand
    SITTING_OUT, // Temporarily sitting out
    DISCONNECTED, // Lost connection
}

data class PlayerState(
    val player: Player,
    val chips: ChipAmount,
    val holeCards: List<com.aaronchancey.poker.kpoker.core.Card> = emptyList(),
    val status: PlayerStatus = PlayerStatus.WAITING,
    val currentBet: ChipAmount = 0,
    val totalBetThisRound: ChipAmount = 0,
    val isDealer: Boolean = false,
    val isSmallBlind: Boolean = false,
    val isBigBlind: Boolean = false,
    val hasActed: Boolean = false,
    val timeBank: Int = 30, // seconds
) {
    val isActive: Boolean get() = status == PlayerStatus.ACTIVE
    val canAct: Boolean get() = status == PlayerStatus.ACTIVE && !hasActed

    fun withChips(amount: ChipAmount) = copy(chips = amount)
    fun withStatus(newStatus: PlayerStatus) = copy(status = newStatus)
    fun withBet(amount: ChipAmount) = copy(currentBet = amount, totalBetThisRound = totalBetThisRound + amount)
    fun withHoleCards(cards: List<com.aaronchancey.poker.kpoker.core.Card>) = copy(holeCards = cards)
    fun clearBet() = copy(currentBet = 0)
    fun markActed() = copy(hasActed = true)
    fun resetForNewRound() = copy(hasActed = false, currentBet = 0)
    fun resetForNewHand() = copy(
        holeCards = emptyList(),
        status = PlayerStatus.WAITING,
        currentBet = 0,
        totalBetThisRound = 0,
        hasActed = false,
        isDealer = false,
        isSmallBlind = false,
        isBigBlind = false,
    )
}
