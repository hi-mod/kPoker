package com.aaronchancey.poker.kpoker.visibility

import com.aaronchancey.poker.kpoker.game.GamePhase
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.player.PlayerId

/**
 * Standard visibility rules for traditional online poker.
 *
 * Rules:
 * - Players see their own hole cards at all times
 * - Other players' hole cards are hidden until showdown
 * - Spectators see no hole cards until showdown
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

                    // Viewer's own seat - show cards
                    seat.playerState.player.id == viewerId -> seat

                    // Showdown - all cards visible
                    state.phase == GamePhase.SHOWDOWN -> seat

                    // Hide other players' cards
                    else -> seat.copy(
                        playerState = seat.playerState.copy(holeCards = emptyList()),
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

                    // Hide all hole cards from spectators
                    else -> seat.copy(
                        playerState = seat.playerState.copy(holeCards = emptyList()),
                    )
                }
            },
        )

        return state.copy(table = visibleTable)
    }
}
