package com.aaronchancey.poker.presentation.room.components

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aaronchancey.poker.kpoker.player.PlayerState

/**
 * A seat slot that renders either an empty seat button or an occupied seat.
 *
 * This wrapper encapsulates the decision logic between showing a "Take Seat"
 * button for empty seats vs rendering the [OccupiedSeat] for occupied ones.
 *
 * @param seatNumber The seat number (1-based)
 * @param playerState The player occupying this seat, or null if empty
 * @param isLoading Whether the room is in a loading state (disables interaction)
 * @param isLocalPlayerSeated Whether the local player is already seated at the table
 * @param onTakeSeat Callback when the user clicks "Take Seat" on an empty seat
 */
@Composable
fun SeatSlot(
    modifier: Modifier = Modifier,
    seatNumber: Int,
    playerState: PlayerState?,
    isLoading: Boolean,
    isLocalPlayerSeated: Boolean,
    onTakeSeat: (Int) -> Unit,
) {
    if (playerState != null) {
        OccupiedSeat(
            modifier = modifier,
            player = playerState,
        )
    } else if (!isLocalPlayerSeated) {
        Button(
            modifier = modifier,
            onClick = { onTakeSeat(seatNumber) },
            enabled = !isLoading,
        ) {
            Text("Take Seat $seatNumber")
        }
    }
    // Empty seats show nothing when local player is already seated
}
