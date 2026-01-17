package com.aaronchancey.poker.kpoker.visibility

import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.player.PlayerId

/**
 * Determines what game state information is visible to each viewer.
 *
 * This interface enables different visibility implementations:
 * - [StandardVisibility]: Traditional rules where players see own cards, others hidden
 * - Future: Mental poker where visibility is enforced cryptographically
 *
 * Both client and server can use this to filter state appropriately.
 */
interface VisibilityService {
    /**
     * Filter game state to show only what a specific player should see.
     *
     * Typically:
     * - Player sees their own hole cards
     * - Other players' hole cards are hidden (unless showdown)
     * - All community cards are visible
     *
     * @param state Full game state with all information
     * @param viewerId The player requesting the view
     * @return Filtered game state appropriate for this viewer
     */
    fun getVisibleState(state: GameState, viewerId: PlayerId): GameState

    /**
     * Filter game state for a spectator (non-player).
     *
     * Typically:
     * - All hole cards are hidden (unless showdown)
     * - All community cards and betting info visible
     *
     * @param state Full game state with all information
     * @return Filtered game state appropriate for spectators
     */
    fun getSpectatorView(state: GameState): GameState
}
