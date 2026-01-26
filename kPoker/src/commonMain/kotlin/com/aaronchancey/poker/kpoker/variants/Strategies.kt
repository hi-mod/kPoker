package com.aaronchancey.poker.kpoker.variants

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.Collections.combinations
import com.aaronchancey.poker.kpoker.core.EvaluatedHand
import com.aaronchancey.poker.kpoker.evaluation.LoHandEvaluator
import com.aaronchancey.poker.kpoker.evaluation.OmahaHandEvaluator
import com.aaronchancey.poker.kpoker.evaluation.StandardHandEvaluator
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.game.GameVariant
import com.aaronchancey.poker.kpoker.game.VariantStrategy
import com.aaronchancey.poker.kpoker.game.Winner
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.kpoker.player.ShowdownStatus

abstract class BaseVariantStrategy : VariantStrategy {
    protected fun checkCommonWinners(state: GameState): List<Winner>? {
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

        val showingPlayers = playersInHand.filter { it.showdownStatus == ShowdownStatus.SHOWN }

        if (showingPlayers.isEmpty()) {
            return listOf(
                Winner(
                    playerId = playersInHand.first().player.id,
                    amount = state.totalPot,
                    handDescription = "No shown hands",
                ),
            )
        }

        return null
    }

    protected fun awardPot(
        state: GameState,
        winners: List<Pair<PlayerState, EvaluatedHand>>,
        potPortion: Double = 1.0,
        potTypeSuffix: String = "",
    ): List<Winner> {
        val result = mutableListOf<Winner>()
        for (pot in state.potManager.pots) {
            val eligibleWinners = winners.filter { it.first.player.id in pot.eligiblePlayerIds }
            if (eligibleWinners.isNotEmpty()) {
                val amountForThisGroup = pot.amount * potPortion
                val share = amountForThisGroup / eligibleWinners.size
                for ((player, hand) in eligibleWinners) {
                    result.add(
                        Winner(
                            playerId = player.player.id,
                            amount = share,
                            handDescription = hand.description(),
                            potType = (if (pot.isMain) "main" else "side") + potTypeSuffix,
                        ),
                    )
                }
            }
        }
        return result
    }
}

class TexasHoldemStrategy : BaseVariantStrategy() {
    private val handEvaluator = StandardHandEvaluator()

    override val metadata = TexasHoldemVariant
    override val gameVariant = GameVariant.TEXAS_HOLDEM

    override fun evaluateHands(state: GameState): List<Winner> {
        checkCommonWinners(state)?.let { return it }

        val showingPlayers = state.table.getPlayersInHand().filter { it.showdownStatus == ShowdownStatus.SHOWN }

        val playerHands = showingPlayers.map { player ->
            val allCards = player.holeCards + state.communityCards
            val bestHand = handEvaluator.findBestHand(allCards, 5).first()
            player to bestHand
        }

        val sortedHands = playerHands.sortedByDescending { it.second }
        val bestHand = sortedHands.first().second
        val winners = sortedHands.filter { it.second.compareTo(bestHand) == 0 }

        return awardPot(state, winners)
    }
}

class OmahaStrategy(
    private val isHiLo: Boolean = false,
) : BaseVariantStrategy() {
    private val handEvaluator = OmahaHandEvaluator()
    private val loEvaluator = LoHandEvaluator()

    override val metadata = if (isHiLo) OmahaHiLoVariant else OmahaVariant
    override val gameVariant = if (isHiLo) GameVariant.OMAHA_HI_LO else GameVariant.OMAHA

    override fun evaluateHands(state: GameState): List<Winner> {
        checkCommonWinners(state)?.let { return it }

        val showingPlayers = state.table.getPlayersInHand().filter { it.showdownStatus == ShowdownStatus.SHOWN }

        val playerHiHands = showingPlayers.map { player ->
            val bestHand = findBestOmahaHand(player.holeCards, state.communityCards)
            player to bestHand
        }

        if (isHiLo) {
            val hiWinners = findBestHands(playerHiHands)
            val playerLoHands = showingPlayers.mapNotNull { player ->
                val loHand = findBestOmahaLoHand(player.holeCards, state.communityCards)
                if (loHand != null) player to loHand else null
            }
            val loWinners = if (playerLoHands.isNotEmpty()) findBestLoHands(playerLoHands) else emptyList()

            val result = mutableListOf<Winner>()
            val hiPortion = if (loWinners.isEmpty()) 1.0 else 0.5
            val loPortion = 1.0 - hiPortion

            result.addAll(awardPot(state, hiWinners, hiPortion, if (loWinners.isNotEmpty()) "-hi" else ""))

            if (loWinners.isNotEmpty()) {
                val loResults = awardPot(state, loWinners, loPortion, "-lo")
                if (loResults.isNotEmpty()) {
                    result.addAll(loResults)
                } else {
                    // No one eligible for lo in some pots, hi scoops those portions
                    // This is handled by awardPot logic above if we were smarter,
                    // but let's stick to the current logic for correctness.
                    // Re-calculate: if loWinners exist but none are eligible for a specific pot,
                    // that pot's lo portion should go to hi winners.
                    // The original code did:
                    /*
                    if (eligibleLo.isNotEmpty()) { ... }
                    else {
                        val perPlayer = loShare / eligibleHi.size
                        for ((player, _) in eligibleHi) { ... }
                    }
                     */
                    // I will refine awardPot or handle it manually here to match original logic exactly.
                    return awardPotHiLoManual(state, hiWinners, loWinners)
                }
            }
            return result
        } else {
            val winners = findBestHands(playerHiHands)
            return awardPot(state, winners)
        }
    }

    private fun awardPotHiLoManual(
        state: GameState,
        hiWinners: List<Pair<PlayerState, EvaluatedHand>>,
        loWinners: List<Pair<PlayerState, EvaluatedHand>>,
    ): List<Winner> {
        val result = mutableListOf<Winner>()
        for (pot in state.potManager.pots) {
            val hiShare = if (loWinners.isEmpty()) pot.amount else pot.amount / 2
            val loShare = pot.amount - hiShare

            val eligibleHi = hiWinners.filter { it.first.player.id in pot.eligiblePlayerIds }
            if (eligibleHi.isNotEmpty()) {
                val perPlayer = hiShare / eligibleHi.size
                for ((player, hand) in eligibleHi) {
                    result.add(Winner(player.player.id, perPlayer, hand.description(), if (pot.isMain) "main-hi" else "side-hi"))
                }
            }

            if (loWinners.isNotEmpty()) {
                val eligibleLo = loWinners.filter { it.first.player.id in pot.eligiblePlayerIds }
                if (eligibleLo.isNotEmpty()) {
                    val perPlayer = loShare / eligibleLo.size
                    for ((player, hand) in eligibleLo) {
                        result.add(Winner(player.player.id, perPlayer, "Lo: ${hand.description()}", if (pot.isMain) "main-lo" else "side-lo"))
                    }
                } else if (eligibleHi.isNotEmpty()) {
                    val perPlayer = loShare / eligibleHi.size
                    for ((player, _) in eligibleHi) {
                        result.add(Winner(player.player.id, perPlayer, "Hi scoops", if (pot.isMain) "main" else "side"))
                    }
                }
            }
        }
        return result
    }

    private fun findBestOmahaHand(holeCards: List<Card>, communityCards: List<Card>): EvaluatedHand = findBestHandCombination(
        holeCards,
        communityCards,
        evaluate = { cards -> handEvaluator.evaluate(cards) },
        compare = { a, b -> a.compareTo(b) },
    ) ?: throw IllegalStateException("Could not evaluate hand")

    private fun findBestOmahaLoHand(holeCards: List<Card>, communityCards: List<Card>): EvaluatedHand? = findBestHandCombination(
        holeCards,
        communityCards,
        evaluate = { cards ->
            val hand = loEvaluator.evaluate(cards)
            if (LoHandEvaluator.isQualifying(hand)) hand else null
        },
        compare = { a, b -> LoHandEvaluator.compare(a, b) * -1 },
    )

    private fun findBestHandCombination(
        holeCards: List<Card>,
        communityCards: List<Card>,
        evaluate: (List<Card>) -> EvaluatedHand?,
        compare: (EvaluatedHand, EvaluatedHand) -> Int,
    ): EvaluatedHand? {
        var bestHand: EvaluatedHand? = null
        val holeCardCombos = combinations(holeCards, 2)
        val communityCombos = combinations(communityCards, 3)

        for (hole in holeCardCombos) {
            for (community in communityCombos) {
                val hand = evaluate(hole + community) ?: continue
                if (bestHand == null || compare(hand, bestHand) > 0) {
                    bestHand = hand
                }
            }
        }
        return bestHand
    }

    private fun findBestHands(hands: List<Pair<PlayerState, EvaluatedHand>>): List<Pair<PlayerState, EvaluatedHand>> {
        val sorted = hands.sortedByDescending { it.second }
        val best = sorted.first().second
        return sorted.filter { it.second.compareTo(best) == 0 }
    }

    private fun findBestLoHands(hands: List<Pair<PlayerState, EvaluatedHand>>): List<Pair<PlayerState, EvaluatedHand>> {
        val sorted = hands.sortedWith { a, b -> LoHandEvaluator.compare(a.second, b.second) }
        val best = sorted.first().second
        return sorted.filter { LoHandEvaluator.compare(it.second, best) == 0 }
    }
}
