package com.aaronchancey.poker.kpoker.evaluation

import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.EvaluatedHand

interface HandEvaluator {
    fun evaluate(cards: List<Card>): EvaluatedHand
    fun findBestHand(cards: List<Card>, handSize: Int = 5): EvaluatedHand
}
