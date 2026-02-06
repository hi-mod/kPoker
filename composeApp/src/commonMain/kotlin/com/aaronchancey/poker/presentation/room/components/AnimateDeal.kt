package com.aaronchancey.poker.presentation.room.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aaronchancey.poker.kpoker.core.DealtCard
import com.aaronchancey.poker.kpoker.player.Seat

@Composable
fun AnimateDeal(
    occupiedSeats: List<Seat>,
    ellipse: EllipseGeometry,
    dealCards: Int,
    tableCenterX: Dp,
    tableCenterY: Dp,
    onAnimatedComplete: () -> Unit = {},
) {
    val windowInfo = LocalWindowInfo.current
    val windowSize = windowInfo.containerSize
    val cardHeight = remember(windowSize) {
        minOf(windowSize.width, windowSize.height) * 0.125f
    }
    val dealProgress = remember { Animatable(0f) }
    var currentCardIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(dealCards) {
        if (dealCards > 0) {
            repeat(dealCards) { index ->
                currentCardIndex = index
                dealProgress.snapTo(0f)
                dealProgress.animateTo(1f, tween(600, easing = FastOutSlowInEasing))
            }
            onAnimatedComplete()
        }
    }

    if (dealCards > 0) {
        occupiedSeats.forEach { seat ->
            val (targetX, targetY) = ellipse.positionForSeat(seat.number)

            // Render previously dealt cards at their final seat position with fan layout
            for (cardIndex in 0 until currentCardIndex) {
                val dealtCard = seat.playerState?.dealtCards?.getOrNull(cardIndex)
                LayoutCenteredAt(x = targetX, y = targetY) {
                    DealCard(
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = cardIndex * 8f
                                rotationZ = cardIndex * 10f
                            }
                            .requiredHeight(cardHeight.dp),
                        dealtCard = dealtCard,
                    )
                }
            }

            // Animate the current card from center to seat
            val progress = dealProgress.value
            val currentX = tableCenterX + (targetX - tableCenterX) * progress
            val currentY = tableCenterY + (targetY - tableCenterY) * progress

            // Flip starts at 80% of the flight — rotationY goes 180 → 0
            val flipRotationY = 180f * (1f - ((progress - 0.8f) / 0.2f).coerceIn(0f, 1f))
            val dealtCard = seat.playerState?.dealtCards?.getOrNull(currentCardIndex)
            val showFace = flipRotationY < 90f && dealtCard != null

            LayoutCenteredAt(x = currentX, y = currentY) {
                DealCard(
                    modifier = Modifier
                        .graphicsLayer {
                            rotationZ = progress * 720f + currentCardIndex * 10f * progress
                            rotationY = flipRotationY
                            translationX = currentCardIndex * 8f * progress
                            cameraDistance = 12f * density
                        }
                        .requiredHeight(cardHeight.dp),
                    dealtCard = if (showFace) dealtCard else null,
                )
            }
        }
    }
}

@Composable
private fun DealCard(
    modifier: Modifier = Modifier,
    dealtCard: DealtCard?,
) {
    if (dealtCard != null) {
        DealtCardView(modifier = modifier, dealtCard = dealtCard)
    } else {
        CardBack(modifier)
    }
}
