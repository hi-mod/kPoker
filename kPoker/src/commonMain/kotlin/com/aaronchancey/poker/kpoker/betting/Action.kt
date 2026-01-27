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

    /**
     * Player chooses to reveal their hole cards at showdown.
     * Required to be eligible for pot.
     */
    @Serializable
    @SerialName("show")
    data class Show(override val playerId: PlayerId) : Action()

    /**
     * Player chooses to muck (hide) their hole cards at showdown.
     * Forfeits claim to pot but keeps cards private.
     */
    @Serializable
    @SerialName("muck")
    data class Muck(override val playerId: PlayerId) : Action()

    /**
     * Player collects pot without revealing cards.
     * Only valid when all other players have mucked (last player standing).
     */
    @Serializable
    @SerialName("collect")
    data class Collect(override val playerId: PlayerId) : Action()
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
    val bettingType: BettingType,
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

@Serializable
enum class ShowdownActionType {
    SHOW,
    MUCK,

    /** Collect pot without showing - only valid when all others mucked */
    COLLECT,
}

/**
 * Request for a showdown action from a player.
 * @param playerId The player who must act
 * @param validActions Available actions:
 *   - SHOW: always available
 *   - MUCK: available if not required to show (forfeits pot claim)
 *   - COLLECT: available only when all others mucked (wins without showing)
 * @param mustShow True if player was called and must show to claim pot
 * @param isLastPlayerStanding True if all others mucked - player wins regardless
 * @param timeLimit Time allowed for decision before auto-muck/auto-collect
 */
@Serializable
data class ShowdownRequest(
    val playerId: PlayerId,
    val validActions: Set<ShowdownActionType>,
    val mustShow: Boolean,
    val isLastPlayerStanding: Boolean = false,
    val timeLimit: Duration = 15.seconds,
)
