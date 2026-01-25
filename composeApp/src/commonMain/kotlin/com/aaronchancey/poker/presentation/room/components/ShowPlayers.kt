package com.aaronchancey.poker.presentation.room.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aaronchancey.poker.presentation.room.AnimatingBet
import com.aaronchancey.poker.presentation.room.PlayerActions
import com.aaronchancey.poker.presentation.room.RoomIntent
import com.aaronchancey.poker.presentation.room.RoomUiState
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ShowPlayers(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    uiState: RoomUiState,
    animatingBets: List<AnimatingBet>,
    onAnimationComplete: (Int) -> Unit,
    onTakeSeat: (Int) -> Unit,
    onIntent: (RoomIntent) -> Unit,
) = Column(
    modifier = modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
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

        // Rail ellipse proportions from pokerTable.svg (viewBox 1200x700, rail at cx=600 cy=330 rx=560 ry=290)
        val centerX = maxWidth / 2 // Rail is horizontally centered (600/1200 = 0.5)
        val centerY = tablePadding + imageHeight * (330f / 700f) // Rail center is at y=330 in 700-height viewBox
        val radiusX = imageWidth * (560f / 1200f) // Rail rx=560 in 1200-width viewBox
        val radiusY = imageHeight * (290f / 700f) // Rail ry=290 in 700-height viewBox

        val playerCount = uiState.gameState?.table?.seats?.size ?: 1
        val angleStep = 360.0 / playerCount

        LayoutCenteredAt(x = maxWidth / 2 - 8.dp, y = maxHeight / 2 - 8.dp) {
            CommunityCards(communityCards = uiState.gameState?.communityCards ?: emptyList())
        }
        WagerChips(
            wager = uiState.gameState?.totalPot ?: 0.0,
            chipOffsetX = maxWidth / 2 - 8.dp,
            chipOffsetY = maxHeight / 2 + 58.dp,
        )

        // Calculate bet position for a given seat number
        val getBetPosition: (Int) -> Pair<Dp, Dp> = remember(
            centerX,
            centerY,
            radiusX,
            radiusY,
            angleStep,
        ) {
            { seatNumber: Int ->
                val angle = ((seatNumber - 1) * angleStep).toRadians()
                val chipFactor = 0.5f
                val x = centerX + radiusX * cos(angle).toFloat() * chipFactor
                val y = centerY + radiusY * sin(angle).toFloat() * chipFactor
                x to y
            }
        }

        val potCenter = (maxWidth / 2 - 8.dp) to (maxHeight / 2 + 58.dp)

        // Render animating chips OUTSIDE seat loop for proper state isolation
        AnimatingChipStacks(
            animatingBets = animatingBets,
            getSeatPosition = getBetPosition,
            potCenter = potCenter,
            onAnimationComplete = onAnimationComplete,
        )

        uiState.gameState?.table?.seats?.forEach { seat ->
            key(seat.number) {
                // Stable key per seat
                val angle = ((seat.number - 1) * angleStep).toRadians()
                val playerX = centerX + radiusX * cos(angle).toFloat()
                val playerY = centerY + radiusY * sin(angle).toFloat()

                LayoutCenteredAt(x = playerX, y = playerY) {
                    val playerState = seat.playerState
                    if (playerState == null) {
                        Button(
                            onClick = { onTakeSeat(seat.number) },
                            enabled = !isLoading,
                        ) {
                            Text("Take Seat ${seat.number}")
                        }
                    } else {
                        ShowPlayer(player = playerState)
                    }
                }

                val screenSize = minOf(maxWidth, maxHeight)
                val currentBet = seat.playerState?.currentBet ?: 0.0

                // Static chips for current bet (not animating)
                if (currentBet > 0.0) {
                    val (chipX, chipY) = getBetPosition(seat.number)
                    WagerChips(
                        wager = currentBet,
                        chipOffsetX = chipX,
                        chipOffsetY = chipY,
                    )
                }

                if (seat.number == uiState.gameState.dealerSeatNumber) {
                    val dealerButtonFactor = 0.72f
                    val dealerButtonX = centerX + radiusX * cos(angle).toFloat() * dealerButtonFactor
                    val dealerButtonY = centerY + radiusY * sin(angle).toFloat() * dealerButtonFactor
                    val size = screenSize / 18
                    LayoutCenteredAt(x = dealerButtonX, y = dealerButtonY) {
                        DealerButton(size = size)
                    }
                }
            }
        }
    }
    Row(
        modifier = Modifier.heightIn(105.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        uiState.gameState?.currentActor?.let { currentActor ->
            PlayerActions(
                playerState = currentActor,
                uiState = uiState,
                onIntent = onIntent,
            )
            Text(uiState.handDescription)
        }
    }
}
