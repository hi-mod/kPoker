package com.aaronchancey.poker.kpoker.game

import com.aaronchancey.poker.kpoker.variants.PokerVariant

interface VariantStrategy {
    val metadata: PokerVariant
    val gameVariant: GameVariant

    fun evaluateHands(state: GameState): List<Winner>
}
