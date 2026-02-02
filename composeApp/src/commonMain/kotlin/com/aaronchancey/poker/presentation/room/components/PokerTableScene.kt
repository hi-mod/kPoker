package com.aaronchancey.poker.presentation.room.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aaronchancey.poker.presentation.room.RoomIntent
import com.aaronchancey.poker.presentation.room.RoomUiState

/**
 * Main poker table scene displaying the game layout.
 *
 * Orchestrates the table visual elements:
 * - Table background with elliptical player seating
 * - Community cards and pot display at center
 * - Chip animations between seats and pot
 * - Player action bar at bottom
 *
 * @param isLoading Whether the room is in a loading state
 * @param uiState Current room UI state
 * @param onTakeSeat Callback when a player takes an empty seat
 * @param onIntent Handler for player action intents
 */
@Composable
fun PokerTableScene(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    uiState: RoomUiState,
    onTakeSeat: (Int) -> Unit,
    onIntent: (RoomIntent) -> Unit,
) = Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    val handNumber = uiState.gameState?.handNumber ?: 0L
    val chipAnimationState = rememberChipAnimationState(handNumber)

    BoxWithConstraints(
        modifier = Modifier
            .weight(1f)
            .background(color = Color.Green),
    ) {
        DrawTable()

        // The table image has 16.dp padding and uses FillBounds
        val tablePadding = 16.dp
        val imageWidth = maxWidth - tablePadding * 2
        val imageHeight = maxHeight - tablePadding * 2

        val playerCount = uiState.gameState?.table?.seats?.size ?: 1

        // Rail ellipse proportions from pokerTable.svg (viewBox 1200x700, rail at cx=600 cy=330 rx=560 ry=290)
        val ellipse = remember(maxWidth, maxHeight, playerCount) {
            EllipseGeometry(
                centerX = maxWidth / 2, // Rail is horizontally centered (600/1200 = 0.5)
                centerY = tablePadding + imageHeight * (330f / 700f), // Rail center is at y=330 in 700-height viewBox
                radiusX = imageWidth * (560f / 1200f), // Rail rx=560 in 1200-width viewBox
                radiusY = imageHeight * (290f / 700f), // Rail ry=290 in 700-height viewBox
                angleStep = 360.0 / playerCount,
            )
        }

        val density = LocalDensity.current
        var measurements by remember { mutableStateOf<TableCenterMeasurements?>(null) }
        val tableCenterX = maxWidth / 2 - 8.dp

        // Community cards and pot display
        LayoutCenteredAt(x = tableCenterX, y = maxHeight / 2) {
            val animatingTotal = chipAnimationState.animatingWinnings.sumOf { it.amount }
            val displayedPot = (uiState.gameState?.totalPot ?: 0.0) - animatingTotal - chipAnimationState.completedWinningsTotal

            TableCenter(
                communityCards = uiState.gameState?.communityCards ?: emptyList(),
                displayedPot = displayedPot,
                onMeasured = { newMeasurements ->
                    // Only update state if values changed to prevent recomposition loop
                    if (measurements != newMeasurements) {
                        measurements = newMeasurements
                    }
                },
            )
        }

        // Calculate pot center for animations
        val potCenterY = measurements?.let { m ->
            with(density) {
                maxHeight / 2 + (m.chipOffsetInColumn + m.chipHeight / 2 - m.columnHeight / 2).toDp()
            }
        } ?: (maxHeight / 2)
        val potCenter = tableCenterX to potCenterY

        // Render animating chips OUTSIDE seat loop for proper state isolation
        // Bets → pot animation
        AnimatingChipStacks(
            animatingBets = chipAnimationState.animatingBets,
            getSeatPosition = { seatNumber -> ellipse.positionForSeat(seatNumber, scaleFactor = 0.5f) },
            potCenter = potCenter,
            onAnimationComplete = { seatNumber -> chipAnimationState.onBetAnimationComplete(seatNumber) },
        )

        // Pot → winners animation
        AnimatingChipStacks(
            animatingBets = chipAnimationState.animatingWinnings,
            getSeatPosition = { seatNumber -> ellipse.positionForSeat(seatNumber, scaleFactor = 0.5f) },
            potCenter = potCenter,
            onAnimationComplete = { seatNumber -> chipAnimationState.onWinningsAnimationComplete(seatNumber) },
            fromPot = true,
        )

        val dealerPosAnim = remember { Animatable((uiState.gameState?.dealerSeatNumber ?: 1).toFloat()) }
        LaunchedEffect(uiState.gameState?.dealerSeatNumber) {
            val targetSeat = uiState.gameState?.dealerSeatNumber ?: return@LaunchedEffect
            val current = dealerPosAnim.value
            val forwardDist = (targetSeat - current.toInt() + 9) % 9
            if (forwardDist > 0) {
                dealerPosAnim.animateTo(current + forwardDist)
            }
        }
        val a = ellipse.positionForSeat(dealerPosAnim.value.toInt(), scaleFactor = 0.72f)
        val dealerX by animateDpAsState(a.first)
        val dealerY by animateDpAsState(a.second)

        uiState.gameState?.let { gameState ->
            val isLocalPlayerSeated = uiState.playerId?.let { gameState.table.getPlayerSeat(it) } != null

            gameState.table.seats.forEach { seat ->
                key(seat.number) {
                    val (playerX, playerY) = ellipse.positionForSeat(seat.number)

                    LayoutCenteredAt(x = playerX, y = playerY) {
                        SeatSlot(
                            seatNumber = seat.number,
                            playerState = seat.playerState,
                            isLoading = isLoading,
                            isLocalPlayerSeated = isLocalPlayerSeated,
                            onTakeSeat = onTakeSeat,
                        )
                    }

                    val currentBet = seat.playerState?.currentBet ?: 0.0
                    if (currentBet > 0.0) {
                        val (chipX, chipY) = ellipse.positionForSeat(seat.number, scaleFactor = 0.5f)
                        WagerChips(
                            wager = currentBet,
                            chipOffsetX = chipX,
                            chipOffsetY = chipY,
                        )
                    }

                    if (seat.number == gameState.dealerSeatNumber) {
                        val buttonSize = minOf(maxWidth, maxHeight) / 18
                        LayoutCenteredAt(x = dealerX, y = dealerY) {
                            DealerButton(size = buttonSize)
                        }
                    }
                }
            }
        }
    }

    ActionBar(
        currentActor = uiState.gameState?.currentActor,
        uiState = uiState,
        handDescription = uiState.handDescription,
        onIntent = onIntent,
    )
}
