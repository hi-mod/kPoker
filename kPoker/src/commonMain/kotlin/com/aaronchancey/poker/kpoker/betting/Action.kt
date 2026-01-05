package com.aaronchancey.poker.kpoker.betting

import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId

sealed class Action {
    abstract val playerId: PlayerId

    data class Fold(override val playerId: PlayerId) : Action()

    data class Check(override val playerId: PlayerId) : Action()

    data class Call(
        override val playerId: PlayerId,
        val amount: ChipAmount,
    ) : Action()

    data class Bet(
        override val playerId: PlayerId,
        val amount: ChipAmount,
    ) : Action()

    data class Raise(
        override val playerId: PlayerId,
        val amount: ChipAmount,
        val totalBet: ChipAmount,
    ) : Action()

    data class AllIn(
        override val playerId: PlayerId,
        val amount: ChipAmount,
    ) : Action()

    data class PostBlind(
        override val playerId: PlayerId,
        val amount: ChipAmount,
        val blindType: BlindType,
    ) : Action()
}

enum class BlindType {
    SMALL_BLIND,
    BIG_BLIND,
    ANTE,
    STRADDLE,
}

data class ActionRequest(
    val playerId: PlayerId,
    val validActions: Set<ActionType>,
    val minimumBet: ChipAmount,
    val minimumRaise: ChipAmount,
    val maximumBet: ChipAmount,
    val amountToCall: ChipAmount,
    val timeLimit: Int = 30, // seconds
)

enum class ActionType {
    FOLD,
    CHECK,
    CALL,
    BET,
    RAISE,
    ALL_IN,
}
