package com.aaronchancey.poker.presentation.room

import com.aaronchancey.poker.kpoker.player.PlayerId

/**
 * Parameters required to initialize a RoomViewModel.
 * Passed via Koin parametersOf() from the platform layer.
 *
 * This bundles the room connection info that was previously passed
 * through the UI layer via LaunchedEffect and JoinRoom intent.
 */
data class RoomParams(
    val roomId: String,
    val playerName: String,
    val playerId: PlayerId,
)
