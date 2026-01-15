package com.aaronchancey.poker.kpoker.variants

import com.aaronchancey.poker.kpoker.betting.BettingStructure
import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.Collections.combinations
import com.aaronchancey.poker.kpoker.core.EvaluatedHand
import com.aaronchancey.poker.kpoker.core.Rank
import com.aaronchancey.poker.kpoker.evaluation.LoHandEvaluator
import com.aaronchancey.poker.kpoker.evaluation.OmahaHandEvaluator
import com.aaronchancey.poker.kpoker.events.GameEvent
import com.aaronchancey.poker.kpoker.game.GamePhase
import com.aaronchancey.poker.kpoker.game.GameVariant
import com.aaronchancey.poker.kpoker.game.PokerGame
import com.aaronchancey.poker.kpoker.game.Winner
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.PlayerStatus

object OmahaVariant : PokerVariant {
    override val name = "Omaha"
    override val holeCardCount = 4
    override val usesCommunityCards = true
    override val minPlayers = 2
    override val maxPlayers = 10
    override val description = "Each player receives 4 hole cards. " +
        "Must use exactly 2 hole cards and 3 community cards."
}

object OmahaHiLoVariant : PokerVariant {
    override val name = "Omaha Hi-Lo"
    override val holeCardCount = 4
    override val usesCommunityCards = true
    override val minPlayers = 2
    override val maxPlayers = 10
    override val description = "Omaha split pot variant. " +
        "Best high hand splits with best qualifying low hand (8 or better)."
}

class OmahaGame(
    bettingStructure: BettingStructure,
    private val isHiLo: Boolean = false,
) : PokerGame(bettingStructure, OmahaHandEvaluator()) {

    private val loEvaluator = LoHandEvaluator()

    override val gameVariant = if (isHiLo) GameVariant.OMAHA_HI_LO else GameVariant.OMAHA
    override val variantName = if (isHiLo) OmahaHiLoVariant.name else OmahaVariant.name
    override val holeCardCount = OmahaVariant.holeCardCount
    override val usesCommunityCards = OmahaVariant.usesCommunityCards

    override fun dealHoleCards() {
        state = state.withPhase(GamePhase.DEALING)

        var table = state.table

        for (seat in state.table.occupiedSeats) {
            if (seat.playerState != null) {
                val cards = state.deck.deal(holeCardCount)
                table = table.updateSeat(seat.number) { s ->
                    s.updatePlayerState { ps ->
                        ps.withHoleCards(cards).withStatus(PlayerStatus.ACTIVE)
                    }
                }
                emit(GameEvent.HoleCardsDealt(seat.playerState.player.id, cards))
            }
        }

        state = state.withTable(table)
    }

    override fun evaluateHands(): List<Winner> {
        val playersInHand = state.table.getPlayersInHand()

        if (playersInHand.size == 1) {
            val winner = playersInHand.first()
            return listOf(
                Winner(
                    playerId = winner.player.id,
                    amount = state.totalPot,
                    handDescription = "Last player standing",
                ),
            )
        }

        // Evaluate best Omaha hand for each player (must use exactly 2 hole cards + 3 community)
        val playerHiHands = playersInHand.map { player ->
            val bestHand = findBestOmahaHand(player.holeCards, state.communityCards)
            player to bestHand
        }

        val result = mutableListOf<Winner>()

        if (isHiLo) {
            // Split pot: Hi and Lo
            val hiWinners = findBestHands(playerHiHands)

            val playerLoHands = playersInHand.mapNotNull { player ->
                val loHand = findBestOmahaLoHand(player.holeCards, state.communityCards)
                if (loHand != null) player to loHand else null
            }
            val loWinners = if (playerLoHands.isNotEmpty()) findBestLoHands(playerLoHands) else emptyList()

            for (pot in state.potManager.pots) {
                val hiShare = if (loWinners.isEmpty()) pot.amount else pot.amount / 2
                val loShare = pot.amount - hiShare

                // Award hi portion
                val eligibleHi = hiWinners.filter { it.first.player.id in pot.eligiblePlayerIds }
                if (eligibleHi.isNotEmpty()) {
                    val perPlayer = hiShare / eligibleHi.size
                    for ((player, hand) in eligibleHi) {
                        result.add(Winner(player.player.id, perPlayer, hand.description(), if (pot.isMain) "main-hi" else "side-hi"))
                    }
                }

                // Award lo portion
                if (loWinners.isNotEmpty()) {
                    val eligibleLo = loWinners.filter { it.first.player.id in pot.eligiblePlayerIds }
                    if (eligibleLo.isNotEmpty()) {
                        val perPlayer = loShare / eligibleLo.size
                        for ((player, hand) in eligibleLo) {
                            result.add(Winner(player.player.id, perPlayer, "Lo: ${hand.description()}", if (pot.isMain) "main-lo" else "side-lo"))
                        }
                    } else {
                        // No qualifying lo, hi takes all
                        val perPlayer = loShare / eligibleHi.size
                        for ((player, _) in eligibleHi) {
                            result.add(Winner(player.player.id, perPlayer, "Hi scoops", if (pot.isMain) "main" else "side"))
                        }
                    }
                }
            }
        } else {
            // Hi only
            val winners = findBestHands(playerHiHands)
            for (pot in state.potManager.pots) {
                val eligibleWinners = winners.filter { it.first.player.id in pot.eligiblePlayerIds }
                if (eligibleWinners.isNotEmpty()) {
                    val share = pot.amount / eligibleWinners.size
                    for ((player, hand) in eligibleWinners) {
                        result.add(Winner(player.player.id, share, hand.description(), if (pot.isMain) "main" else "side"))
                    }
                }
            }
        }

        return result
    }

    private fun findBestOmahaHand(holeCards: List<Card>, communityCards: List<Card>): EvaluatedHand {
        var bestHand: EvaluatedHand? = null

        // Must use exactly 2 hole cards
        val holeCardCombos = combinations(holeCards, 2)
        // Must use exactly 3 community cards
        val communityCombos = combinations(communityCards, 3)

        for (hole in holeCardCombos) {
            for (community in communityCombos) {
                val hand = handEvaluator.evaluate(hole + community)
                if (bestHand == null || hand > bestHand) {
                    bestHand = hand
                }
            }
        }

        return bestHand ?: throw IllegalStateException("Could not evaluate hand")
    }

    private fun findBestOmahaLoHand(holeCards: List<Card>, communityCards: List<Card>): EvaluatedHand? {
        var bestHand: EvaluatedHand? = null

        val holeCardCombos = combinations(holeCards, 2)
        val communityCombos = combinations(communityCards, 3)

        for (hole in holeCardCombos) {
            for (community in communityCombos) {
                val cards = hole + community
                // Check if qualifying lo (8 or better, no pairs)
                val values = cards.map { if (it.rank == Rank.ACE) 1 else it.rank.value }
                if (values.distinct().size == 5 && values.max() <= 8) {
                    val hand = loEvaluator.evaluate(cards)
                    if (bestHand == null || compareLo(hand, bestHand) < 0) {
                        bestHand = hand
                    }
                }
            }
        }

        return bestHand
    }

    private fun compareLo(a: EvaluatedHand, b: EvaluatedHand): Int {
        val aValues = a.cards.map { if (it.rank == Rank.ACE) 1 else it.rank.value }.sortedDescending()
        val bValues = b.cards.map { if (it.rank == Rank.ACE) 1 else it.rank.value }.sortedDescending()

        for (i in aValues.indices) {
            val cmp = aValues[i].compareTo(bValues[i])
            if (cmp != 0) return cmp
        }
        return 0
    }

    private fun findBestHands(hands: List<Pair<PlayerState, EvaluatedHand>>): List<Pair<PlayerState, EvaluatedHand>> {
        val sorted = hands.sortedByDescending { it.second }
        val best = sorted.first().second
        return sorted.filter { it.second.compareTo(best) == 0 }
    }

    private fun findBestLoHands(hands: List<Pair<PlayerState, EvaluatedHand>>): List<Pair<PlayerState, EvaluatedHand>> {
        val sorted = hands.sortedWith { a, b -> compareLo(a.second, b.second) }
        val best = sorted.first().second
        return sorted.filter { compareLo(it.second, best) == 0 }
    }

    companion object {
        fun potLimit(
            smallBlind: ChipAmount,
            bigBlind: ChipAmount,
            minDenomination: ChipAmount = 1.0,
        ): OmahaGame = OmahaGame(
            BettingStructure.potLimit(
                smallBlind,
                bigBlind,
                minDenomination = minDenomination,
            ),
        )

        fun potLimitHiLo(
            smallBlind: ChipAmount,
            bigBlind: ChipAmount,
            minDenomination: ChipAmount = 1.0,
        ): OmahaGame = OmahaGame(
            BettingStructure.potLimit(
                smallBlind,
                bigBlind,
                minDenomination = minDenomination,
            ),
            isHiLo = true,
        )
    }
}
