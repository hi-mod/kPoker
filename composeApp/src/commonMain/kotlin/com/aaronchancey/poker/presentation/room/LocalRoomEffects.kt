package com.aaronchancey.poker.presentation.room

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.Flow

/**
 * CompositionLocal providing the room effects Flow to the composition tree.
 *
 * This allows any composable within the room to observe side effects (like
 * [RoomEffect.AnimateChipsToPot]) without drilling callbacks through multiple layers.
 *
 * Must be provided via [CompositionLocalProvider] at the RoomScreen level.
 */
val LocalRoomEffects = staticCompositionLocalOf<Flow<RoomEffect>> {
    error("LocalRoomEffects not provided - wrap your composable tree with CompositionLocalProvider")
}
