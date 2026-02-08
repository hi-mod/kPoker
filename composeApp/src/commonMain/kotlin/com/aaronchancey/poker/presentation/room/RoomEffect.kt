package com.aaronchancey.poker.presentation.room

import com.aaronchancey.poker.kpoker.player.ChipAmount

sealed interface RoomEffect {
    data class DealCards(val numCards: Int) : RoomEffect
    data class ShowToast(val message: String) : RoomEffect
    data object NavigateToLobby : RoomEffect
    data class PlaySound(val soundType: SoundType) : RoomEffect

    /**
     * Triggers chip animation from player bet positions to the pot.
     * Emitted when a betting round ends and bets are collected.
     */
    data class AnimateChipsToPot(val bets: List<AnimatingBet>) : RoomEffect

    /**
     * Triggers chip animation from the pot to winning players.
     * Emitted when a hand completes and winnings are distributed.
     */
    data class AnimateChipsFromPot(val winnings: List<AnimatingBet>) : RoomEffect

    /**
     * Shows ante chips at player seats before animating to pot.
     * Emitted when antes are posted, before the animation begins.
     */
    data class ShowAntesAtSeats(val antes: List<AnimatingBet>) : RoomEffect
}

/**
 * Represents a bet that should animate to the pot.
 *
 * @param seatNumber The seat number of the player whose bet is animating
 * @param amount The bet amount (used to render the correct chips)
 */
data class AnimatingBet(
    val seatNumber: Int,
    val amount: ChipAmount,
)
