package com.poker.domain

import com.poker.statemachine.GameEvent

data class Player @JvmOverloads constructor(
    val id: String = "",
    val name: String = "",
    val chips: Double = 0.0,
    val hand: List<Card> = listOf(),
    val availablePlayerActions: List<GameEvent.SelectPlayerAction> = emptyList(),
    val currentWager: Double = 0.0,
    val hasActed: Boolean = false,
    val hasFolded: Boolean = false,
) {

    fun wager(amount: Double) = copy(
        availablePlayerActions = emptyList(),
        currentWager = currentWager + amount,
        chips = chips - amount,
        hasActed = true,
    )

    fun fold() = copy(hasActed = true, hasFolded = true)

    fun check() = copy(hasActed = true)

    fun wins(amount: Int) = wins(amount.toDouble())

    fun wins(amount: Double) = copy(chips = chips + amount, currentWager = 0.0)

    fun handDescription(board: List<Card>): String {
        val hand = hand.plus(board).sortedByDescending { it.rank }
        val handKind = handKind(hand)
        val bestFiveCardHand = extractBestFiveCardHand(handKind, hand)
        val handRank = handRank(bestFiveCardHand)
        val k1 = (handRank and 0x0f0000) shr 16
        val k2 = (handRank and 0x00f000) shr 12
        val k3 = (handRank and 0x000f00) shr 8
        val k4 = (handRank and 0x0000f0) shr 4
        val k5 = handRank and 0x00000f

        val card = CardRank.entries.toTypedArray()
        val card1ShortName = card.first { it.value == k1 }.shortName
        val card5ShortName = card.first { it.value == k5 }.shortName
        val card1PluralName = card.first { it.value == k1 }.pluralName
        val card3PluralName = card.first { it.value == k3 }.pluralName
        val card4PluralName = card.first { it.value == k4 }.pluralName

        return when {
            handRank == HandKind.RoyalFlush.rank -> HandKind.RoyalFlush.description
            handRank >= HandKind.StraightFlush.rank -> HandKind.StraightFlush.description + ", " + card1ShortName + " to " + card5ShortName
            handRank >= HandKind.FourOfAKind.rank -> HandKind.FourOfAKind.description + ", " + card1PluralName
            handRank >= HandKind.FullHouse.rank -> HandKind.FullHouse.description + ", " + card1PluralName + " full of " + card4PluralName
            handRank >= HandKind.Flush.rank -> HandKind.Flush.description + ", " + card1ShortName + " high"
            handRank >= HandKind.Straight.rank -> HandKind.Straight.description + ", " + card5ShortName + " to " + card1ShortName
            handRank >= HandKind.ThreeOfAKind.rank -> HandKind.ThreeOfAKind.description + ", " + card1PluralName
            handRank >= HandKind.TwoPair.rank -> HandKind.TwoPair.description + ", " + card1PluralName + " and " + card3PluralName
            handRank >= HandKind.Pair.rank -> HandKind.Pair.description + " of " + card1PluralName
            handRank >= HandKind.HighCard.rank -> HandKind.HighCard.description + " " + card1ShortName
            else -> "Error"
        }
    }

    internal fun handRank(hand: List<Card>) = handKind(hand).rank +
        (hand[0].rank.value shl 16) +
        (hand[1].rank.value shl 12) +
        (hand[2].rank.value shl 8) +
        (hand[3].rank.value shl 4) +
        hand[4].rank.value

    internal fun handKind(hand: List<Card>) = when {
        straightFlush(hand) -> HandKind.StraightFlush
        quads(hand) -> HandKind.FourOfAKind
        fullHouse(hand) -> HandKind.FullHouse
        flush(hand) -> HandKind.Flush
        straight(hand) -> HandKind.Straight
        threeOfAKind(hand) -> HandKind.ThreeOfAKind
        twoPair(hand) -> HandKind.TwoPair
        pair(hand) -> HandKind.Pair
        hand.size < 5 -> HandKind.Error
        else -> HandKind.HighCard
    }

    internal fun extractBestFiveCardHand(hankKind: HandKind, hand: List<Card>) = when (hankKind) {
        HandKind.RoyalFlush -> hand.take(5)
        HandKind.StraightFlush -> {
            val handGroupings = hand.groupBy { it.suit }
            handGroupings.filter { it.value.size >= 5 }.flatMap { it.value }
        }
        HandKind.FourOfAKind -> getBestFiveForPairings(hand)
        HandKind.FullHouse -> getBestFiveForPairings(hand)
        HandKind.Flush -> {
            val handGroupings = hand.groupBy { it.suit }
            handGroupings.filter { it.value.size >= 5 }.flatMap { it.value }
        }
        HandKind.Straight -> {
            val handValues = hand.sortedByDescending { it.rank.value }
            handValues.windowed(5).firstOrNull { window ->
                val maxCard = window.maxOf { it.rank.value }
                val minCard = window.minOf { it.rank.value }
                window.distinct().size == 5 && maxCard - minCard + 1 == window.size
            } ?: emptyList()
        }
        HandKind.ThreeOfAKind -> getBestFiveForPairings(hand)
        HandKind.TwoPair -> getBestFiveForPairings(hand)
        HandKind.Pair -> getBestFiveForPairings(hand)
        HandKind.HighCard -> hand.take(5)
        HandKind.Error -> emptyList()
    }

    private fun getBestFiveForPairings(hand: List<Card>) = hand
        .groupBy { it.rank }
        .entries
        .sortedByDescending { it.value.size }
        .flatMap { it.value }
        .take(5)

    private fun straightFlush(hand: List<Card>): Boolean {
        val flush = hand.groupBy { it.suit }.filterValues { it.size >= 5 }.values.flatten()
        return flush.size >= 5 && straight(hand)
    }

    private fun quads(hand: List<Card>): Boolean {
        val handGroupings = hand.groupBy { it.rank }
        return handGroupings.values.any { it.size == 4 }
    }

    private fun fullHouse(hand: List<Card>): Boolean {
        val handGroupings = hand.groupBy { it.rank }
        return handGroupings.values.any { it.size == 3 } && handGroupings.values.any { it.size == 2 }
    }

    private fun flush(hand: List<Card>) =
        hand.groupBy { it.suit }.filterValues { it.size >= 5 }.values.flatten().size >= 5

    private fun straight(hand: List<Card>): Boolean {
        val sortedHandValues = hand.map { it.rank.value }.sorted()
        return sortedHandValues
            .windowed(5)
            .any { it.maxOrNull()!! - it.minOrNull()!! == 4 && it.distinct().size == 5 }
    }

    private fun threeOfAKind(hand: List<Card>): Boolean {
        val handGroupings = hand.groupBy { it.rank }
        return handGroupings.values.any { it.size == 3 }
    }

    private fun twoPair(hand: List<Card>): Boolean {
        val handGroupings = hand.groupBy { it.rank }
        return handGroupings.entries.filter { it.value.size == 2 }.size == 2
    }

    private fun pair(hand: List<Card>): Boolean {
        val handGroupings = hand.groupBy { it.rank }
        return handGroupings.values.any { it.size == 2 }
    }
}
