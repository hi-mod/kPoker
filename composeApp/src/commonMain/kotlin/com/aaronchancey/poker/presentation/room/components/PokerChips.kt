package com.aaronchancey.poker.presentation.room.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.presentation.room.AnimatingBet
import kotlinx.coroutines.delay

internal val Color.Companion.Purple: Color
    get() = Color(0xFF800080)

internal val Color.Companion.Brown: Color
    get() = Color(0xFFA52A2A)

private val colors = mapOf(
    1.0 to Color.Blue,
    5.0 to Color.Red,
    10.0 to Color.Cyan,
    25.0 to Color.Green,
    100.0 to Color.Black,
    500.0 to Color.Purple,
    1000.0 to Color.Yellow,
    5000.0 to Color.Brown,
)

/** Pre-sorted chip values - computed once at class load, not during composition. */
private val sortedChipValues = colors.keys.sortedDescending()

/**
 * Calculates the optimal chip breakdown for a given wager amount.
 * Uses greedy algorithm to minimize total chip count.
 *
 * @param wager The total wager amount to break down
 * @return List of (chipValue, count) pairs representing chip stacks
 */
private fun buildChipStacks(wager: ChipAmount): List<Pair<Double, Int>> {
    val stacks = mutableListOf<Pair<Double, Int>>()
    var remaining = wager
    for (chipValue in sortedChipValues) {
        val count = (remaining / chipValue).toInt()
        if (count > 0) {
            stacks.add(chipValue to count)
            remaining -= chipValue * count
        }
    }
    return stacks
}

/**
 * Renders poker chips stacked by denomination for a given wager amount.
 * Uses memoization to only recalculate chip breakdown when wager changes.
 *
 * @param modifier Modifier for the container Box
 * @param wager The total wager amount to display as chips
 * @param chipOffsetX Horizontal offset for the chip stack group center
 * @param chipOffsetY Vertical offset for the chip stack group center
 */
@Composable
fun WagerChips(
    modifier: Modifier = Modifier,
    wager: ChipAmount,
    chipOffsetX: Dp = 0.dp,
    chipOffsetY: Dp = 0.dp,
) {
    // Memoize chip breakdown - only recalculate when wager changes
    val chipStacks = remember(wager) { buildChipStacks(wager) }

    Box(modifier = modifier) {
        chipStacks.forEachIndexed { stackIndex, (chipValue, count) ->
            key(chipValue) {
                // Stable key per denomination
                val horizontalOffset = 34.dp * stackIndex
                repeat(count) { chipIndex ->
                    val verticalOffset = 4.dp * chipIndex
                    Box(
                        modifier = Modifier.offset {
                            IntOffset(
                                x = (chipOffsetX + horizontalOffset - 17.5.dp).roundToPx(),
                                y = (chipOffsetY + verticalOffset - 17.5.dp).roundToPx(),
                            )
                        },
                    ) {
                        PokerChip(value = chipValue)
                    }
                }
            }
        }
    }
}

/**
 * Renders all animating bet chips moving from player positions to pot center.
 * Animation state is managed with proper key() scoping to prevent state corruption.
 *
 * @param animatingBets List of bets currently animating to the pot
 * @param getSeatPosition Function to get (x, y) position for a seat number
 * @param potCenter The (x, y) center coordinates of the pot
 * @param onAnimationComplete Callback when a seat's animation finishes
 */
@Composable
fun AnimatingChipStacks(
    animatingBets: List<AnimatingBet>,
    getSeatPosition: (Int) -> Pair<Dp, Dp>,
    potCenter: Pair<Dp, Dp>,
    onAnimationComplete: (Int) -> Unit,
) {
    animatingBets.forEach { animatingBet ->
        key(animatingBet.seatNumber, animatingBet.amount) {
            val (startX, startY) = getSeatPosition(animatingBet.seatNumber)
            val (potX, potY) = potCenter

            var animationStarted by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                animationStarted = true
                delay(400) // Animation duration
                onAnimationComplete(animatingBet.seatNumber)
            }

            val progress by animateFloatAsState(
                targetValue = if (animationStarted) 1f else 0f,
                animationSpec = tween(400, easing = FastOutSlowInEasing),
            )

            val currentX = startX + (potX - startX) * progress
            val currentY = startY + (potY - startY) * progress

            WagerChips(
                wager = animatingBet.amount,
                chipOffsetX = currentX,
                chipOffsetY = currentY,
            )
        }
    }
}

@Composable
private fun PokerChip(
    modifier: Modifier = Modifier,
    value: Double = 0.0,
) {
    val gradient = Brush.radialGradient(
        colors = listOf(colors[value] ?: Color.White, Color.DarkGray),
        radius = 35f,
    )

    Box(
        modifier = modifier
            .size(35.dp)
            .padding(2.dp),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(brush = gradient)
            val strokeWidth = size.minDimension / 10
            val halfStrokeWidth = strokeWidth / 2
            val bandColors = listOf(Color.White, Color.Black)
            repeat(16) { index ->
                drawArc(
                    color = bandColors[index % bandColors.size],
                    startAngle = index * 22.5f,
                    sweepAngle = 22.5f,
                    useCenter = false,
                    topLeft = Offset(halfStrokeWidth, halfStrokeWidth),
                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                    style = Stroke(strokeWidth),
                )
            }
        }
        Text(
            modifier = Modifier.align(Alignment.Center),
            text = (if (value % 1 == 0.0) value.toInt() else value).toString(),
            color = Color.White,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                shadow = Shadow(
                    color = Color.Black,
                    blurRadius = 1f,
                    offset = Offset(1f, 1f),
                ),
            ),
            textAlign = TextAlign.Center,
        )
    }
}
