package com.aaronchancey.poker.network

import com.aaronchancey.poker.kpoker.betting.ActionRequest
import com.aaronchancey.poker.kpoker.betting.ShowdownRequest
import com.aaronchancey.poker.kpoker.game.GameState
import com.aaronchancey.poker.kpoker.player.PlayerId
import com.aaronchancey.poker.shared.message.RoomInfo

/**
 * Unified session state for a player's connection to a poker room.
 *
 * This consolidates all server-driven state into a single immutable object,
 * enabling atomic updates and simplifying state observation in the ViewModel.
 *
 * @property playerId The server-assigned player ID (received in Welcome message)
 * @property roomInfo Room metadata including name, config, and player count
 * @property gameState Current game state with table, players, and community cards
 * @property availableActions Actions the player can take (when it's their turn)
 * @property showdown Showdown request when player must reveal cards
 * @property error Most recent error message from the server
 */
data class RoomSession(
    val playerId: PlayerId? = null,
    val roomInfo: RoomInfo? = null,
    val gameState: GameState? = null,
    val availableActions: ActionRequest? = null,
    val showdown: ShowdownRequest? = null,
    val error: String? = null,
) {
    companion object {
        val Initial = RoomSession()
    }
}
