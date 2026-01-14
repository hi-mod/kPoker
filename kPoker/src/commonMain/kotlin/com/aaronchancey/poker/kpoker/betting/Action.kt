package com.aaronchancey.poker.kpoker.betting

import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerId
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class Action {
    abstract val playerId: PlayerId

    @Serializable
    @SerialName("fold")
    data class Fold(override val playerId: PlayerId) : Action()

    @Serializable
    @SerialName("check")
    data class Check(override val playerId: PlayerId) : Action()

    @Serializable
    @SerialName("call")
    data class Call(
        override val playerId: PlayerId,
        val amount: ChipAmount,
    ) : Action()

    @Serializable
    @SerialName("bet")
    data class Bet(
        override val playerId: PlayerId,
        val amount: ChipAmount,
    ) : Action()

    @Serializable
    @SerialName("raise")
    data class Raise(
        override val playerId: PlayerId,
        val amount: ChipAmount,
        val totalBet: ChipAmount,
    ) : Action()

    @Serializable
    @SerialName("all_in")
    data class AllIn(
        override val playerId: PlayerId,
        val amount: ChipAmount,
    ) : Action()

    @Serializable
    @SerialName("post_blind")
    data class PostBlind(
        override val playerId: PlayerId,
        val amount: ChipAmount,
        val blindType: BlindType,
    ) : Action()
}

@Serializable
enum class BlindType {
    SMALL_BLIND,
    BIG_BLIND,
    ANTE,
    STRADDLE,
}

@Serializable
data class ActionRequest(
    val playerId: PlayerId,
    val validActions: Set<ActionType>,
    val minimumBet: ChipAmount,
    val minimumRaise: ChipAmount,
    val maximumBet: ChipAmount,
    val amountToCall: ChipAmount,
    val timeLimit: Duration = 30.seconds,
    val minimumDenomination: ChipAmount,
)

@Serializable
enum class ActionType {
    FOLD,
    CHECK,
    CALL,
    BET,
    RAISE,
    ALL_IN,
}
