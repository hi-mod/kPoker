package com.aaronchancey.poker.kpoker.visibility

import com.aaronchancey.poker.kpoker.core.CardVisibility
import com.aaronchancey.poker.kpoker.core.DealtCard
import com.aaronchancey.poker.kpoker.game.GamePhase
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.kpoker.player.PlayerState

/**
 * Standard visibility rules for traditional online poker.
 *
 * Rules:
 * - Players see their own hole cards at all times
 * - Other players' hole cards are hidden until showdown (unless PUBLIC visibility)
 * - PUBLIC cards (face-up in Stud) are visible to everyone
 * - PRIVATE cards (face-down) show as hidden placeholders to opponents
 * - Spectators see only PUBLIC cards until showdown
 * - Community cards and betting information always visible
 *
 * This is the trust model where the server knows all cards but
 * only sends appropriate information to each client.
 */
class StandardVisibility : VisibilityService {

    override fun getVisibleState(state: GameState, viewerId: PlayerId): GameState {
        val visibleTable = state.table.copy(
            seats = state.table.seats.map { seat ->
                when {
                    // Empty seat - no change
                    seat.playerState == null -> seat

                    // Viewer's own seat - show all cards
                    seat.playerState.player.id == viewerId -> seat

                    // Showdown - all cards visible
                    state.phase == GamePhase.SHOWDOWN -> seat

                    // Filter other players' cards based on per-card visibility
                    else -> seat.copy(
                        playerState = filterPlayerCards(seat.playerState),
                    )
                }
            },
        )

        return state.copy(table = visibleTable)
    }

    override fun getSpectatorView(state: GameState): GameState {
        val visibleTable = state.table.copy(
            seats = state.table.seats.map { seat ->
                when {
                    // Empty seat - no change
                    seat.playerState == null -> seat

                    // Showdown - all cards visible
                    state.phase == GamePhase.SHOWDOWN -> seat

                    // Filter all players' cards based on per-card visibility
                    else -> seat.copy(
                        playerState = filterPlayerCards(seat.playerState),
                    )
                }
            },
        )

        return state.copy(table = visibleTable)
    }

    /**
     * Filters a player's cards based on per-card visibility rules.
     * PUBLIC cards retain their values, PRIVATE cards become hidden placeholders.
     */
    private fun filterPlayerCards(playerState: PlayerState): PlayerState {
        val filteredDealtCards = playerState.dealtCards.map { dealt ->
            when (dealt.visibility) {
                CardVisibility.PUBLIC -> dealt

                // Keep card value visible
                CardVisibility.PRIVATE -> DealtCard.hidden() // Hide card value
            }
        }

        // Extract visible cards for holeCards (maintaining backward compatibility)
        val visibleHoleCards = filteredDealtCards.mapNotNull { it.card }

        return playerState.copy(
            dealtCards = filteredDealtCards,
            holeCards = visibleHoleCards,
        )
    }
}
