package com.aaronchancey.poker.kpoker.evaluation

import com.aaronchancey.poker.kpoker.game.GameVariant

object HandEvaluatorFactory {
    fun getEvaluator(variant: GameVariant): HandEvaluator = when (variant) {
        GameVariant.TEXAS_HOLDEM -> StandardHandEvaluator()
        GameVariant.OMAHA -> OmahaHandEvaluator()
        GameVariant.OMAHA_HI_LO -> OmahaHiLoHandEvaluator()
    }
}
