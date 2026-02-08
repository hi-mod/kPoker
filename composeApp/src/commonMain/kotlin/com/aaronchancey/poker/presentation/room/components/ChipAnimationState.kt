package com.aaronchancey.poker.presentation.room.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.aaronchancey.poker.presentation.common.ObserveAsEvents
import com.aaronchancey.poker.presentation.room.AnimatingBet
import com.aaronchancey.poker.presentation.room.LocalRoomEffects
import com.aaronchancey.poker.presentation.room.RoomEffect

/**
 * Holds state for chip animations between player seats and the pot.
 *
 * Tracks four categories:
 * - [antesAtSeats]: Antes displayed at seats before animating to pot
 * - [animatingBets]: Chips animating FROM player seats TO the pot
 * - [animatingWinnings]: Chips animating FROM the pot TO winning players
 * - [completedWinningsTotal]: Sum of winnings already animated (keeps pot hidden until new hand)
 */
@Stable
class ChipAnimationState {
    var antesAtSeats by mutableStateOf<List<AnimatingBet>>(emptyList())
    var animatingBets by mutableStateOf<List<AnimatingBet>>(emptyList())
    var animatingWinnings by mutableStateOf<List<AnimatingBet>>(emptyList())
    var completedWinningsTotal by mutableDoubleStateOf(0.0)

    /** Resets animation state for a new hand. */
    fun resetForNewHand() {
        completedWinningsTotal = 0.0
        antesAtSeats = emptyList()
    }

    /** Removes completed bet animation for a seat. */
    fun onBetAnimationComplete(seatNumber: Int) {
        animatingBets = animatingBets.filter { it.seatNumber != seatNumber }
    }

    /** Removes completed winnings animation and tracks the amount. */
    fun onWinningsAnimationComplete(seatNumber: Int) {
        val completed = animatingWinnings.filter { it.seatNumber == seatNumber }
        completedWinningsTotal += completed.sumOf { it.amount }
        animatingWinnings = animatingWinnings.filter { it.seatNumber != seatNumber }
    }
}

/**
 * Creates and remembers a [ChipAnimationState] that automatically observes
 * animation effects from [LocalRoomEffects] and resets on hand number changes.
 *
 * @param handNumber Current hand number - state resets when this changes
 */
@Composable
fun rememberChipAnimationState(handNumber: Long): ChipAnimationState {
    val state = remember { ChipAnimationState() }

    // Reset completed winnings when a new hand starts
    LaunchedEffect(handNumber) {
        state.resetForNewHand()
    }

    // Observe animation effects from LocalRoomEffects
    val effects = LocalRoomEffects.current
    ObserveAsEvents(effects) { effect ->
        when (effect) {
            is RoomEffect.ShowAntesAtSeats -> state.antesAtSeats = effect.antes
            is RoomEffect.AnimateChipsToPot -> {
                // Clear antes at seats when they start animating to pot
                state.antesAtSeats = emptyList()
                state.animatingBets = effect.bets
            }
            is RoomEffect.AnimateChipsFromPot -> state.animatingWinnings = effect.winnings
            else -> {}
        }
    }

    return state
}
