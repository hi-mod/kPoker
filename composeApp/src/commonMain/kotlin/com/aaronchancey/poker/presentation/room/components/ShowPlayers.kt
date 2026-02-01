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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.aaronchancey.poker.presentation.common.ObserveAsEvents
import com.aaronchancey.poker.presentation.room.AnimatingBet
import com.aaronchancey.poker.presentation.room.LocalRoomEffects
import com.aaronchancey.poker.presentation.room.RoomEffect
import com.aaronchancey.poker.presentation.room.RoomIntent
import com.aaronchancey.poker.presentation.room.RoomUiState

@Composable
fun ShowPlayers(
    modifier: Modifier = Modifier,
    isLoading: Boolean,
    uiState: RoomUiState,
    onTakeSeat: (Int) -> Unit,
    onIntent: (RoomIntent) -> Unit,
) {
    // Local animation state - managed here where it's consumed
    var animatingBets by remember { mutableStateOf<List<AnimatingBet>>(emptyList()) }
    var animatingWinnings by remember { mutableStateOf<List<AnimatingBet>>(emptyList()) }
    // Track completed winnings to keep pot hidden until new hand starts
    var completedWinningsTotal by remember { mutableDoubleStateOf(0.0) }

    // Reset completed winnings when a new hand starts
    val handNumber = uiState.gameState?.handNumber ?: 0
    LaunchedEffect(handNumber) {
        completedWinningsTotal = 0.0
    }

    // Observe animation effects from LocalRoomEffects
    val effects = LocalRoomEffects.current
    ObserveAsEvents(effects) { effect ->
        when (effect) {
            is RoomEffect.AnimateChipsToPot -> animatingBets = effect.bets
            is RoomEffect.AnimateChipsFromPot -> animatingWinnings = effect.winnings
            else -> {}
        }
    }

    Column(
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
            var columnHeight by remember { mutableFloatStateOf(0f) }
            var chipOffsetInColumn by remember { mutableFloatStateOf(0f) }
            var chipHeight by remember { mutableFloatStateOf(0f) }

            LayoutCenteredAt(x = maxWidth / 2 - 8.dp, y = maxHeight / 2) {
                Column(
                    modifier = Modifier.onGloballyPositioned { coords ->
                        columnHeight = coords.size.height.toFloat()
                    },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CommunityCards(communityCards = uiState.gameState?.communityCards ?: emptyList())
                    // Reduce displayed pot by animating + already-animated winnings
                    val animatingTotal = animatingWinnings.sumOf { it.amount }
                    val displayedPot = (uiState.gameState?.totalPot ?: 0.0) - animatingTotal - completedWinningsTotal
                    ChipStacks(
                        modifier = Modifier.onGloballyPositioned { coords ->
                            chipOffsetInColumn = coords.positionInParent().y
                            chipHeight = coords.size.height.toFloat()
                        },
                        wager = maxOf(0.0, displayedPot),
                    )
                }
            }

            // Column center is at maxHeight/2 (via LayoutCenteredAt)
            // ChipStacks center = Column center + (chipOffsetInColumn + chipHeight/2 - columnHeight/2)
            val potCenterY = with(density) {
                maxHeight / 2 + (chipOffsetInColumn + chipHeight / 2 - columnHeight / 2).toDp()
            }
            val potCenter = (maxWidth / 2 - 8.dp) to potCenterY

            // Render animating chips OUTSIDE seat loop for proper state isolation
            // Bets → pot animation
            AnimatingChipStacks(
                animatingBets = animatingBets,
                getSeatPosition = { seatNumber -> ellipse.positionForSeat(seatNumber, scaleFactor = 0.5f) },
                potCenter = potCenter,
                onAnimationComplete = { seatNumber ->
                    animatingBets = animatingBets.filter { it.seatNumber != seatNumber }
                },
            )

            // Pot → winners animation
            AnimatingChipStacks(
                animatingBets = animatingWinnings,
                getSeatPosition = { seatNumber -> ellipse.positionForSeat(seatNumber, scaleFactor = 0.5f) },
                potCenter = potCenter,
                onAnimationComplete = { seatNumber ->
                    // Track completed amounts to keep pot hidden until new hand
                    val completed = animatingWinnings.filter { it.seatNumber == seatNumber }
                    completedWinningsTotal += completed.sumOf { it.amount }
                    animatingWinnings = animatingWinnings.filter { it.seatNumber != seatNumber }
                },
                fromPot = true,
            )

            uiState.gameState?.table?.seats?.forEach { seat ->
                key(seat.number) {
                    val (playerX, playerY) = ellipse.positionForSeat(seat.number)

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

                    val currentBet = seat.playerState?.currentBet ?: 0.0
                    if (currentBet > 0.0) {
                        val (chipX, chipY) = ellipse.positionForSeat(seat.number, scaleFactor = 0.5f)
                        WagerChips(
                            wager = currentBet,
                            chipOffsetX = chipX,
                            chipOffsetY = chipY,
                        )
                    }

                    if (seat.number == uiState.gameState.dealerSeatNumber) {
                        val (dealerX, dealerY) = ellipse.positionForSeat(seat.number, scaleFactor = 0.72f)
                        val buttonSize = minOf(maxWidth, maxHeight) / 18
                        LayoutCenteredAt(x = dealerX, y = dealerY) {
                            DealerButton(size = buttonSize)
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
}
