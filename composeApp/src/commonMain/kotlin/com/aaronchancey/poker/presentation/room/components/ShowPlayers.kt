package com.aaronchancey.poker.presentation.room.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.DealtCard
import com.aaronchancey.poker.kpoker.player.ChipAmount
import com.aaronchancey.poker.kpoker.player.PlayerState
import com.aaronchancey.poker.presentation.room.AnimatingBet
import com.aaronchancey.poker.presentation.room.PlayerActions
import com.aaronchancey.poker.presentation.room.RoomIntent
import com.aaronchancey.poker.presentation.room.RoomUiState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import poker.composeapp.generated.resources.Res
import poker.composeapp.generated.resources.pokerTable

/**
 * Places content with its CENTER at the specified (x, y) coordinates.
 *
 * This solves the common UI problem where mathematical calculations (like ellipse positioning)
 * compute center points, but Compose's offset() positions by top-left corner.
 *
 * @param x The x-coordinate where the content's center should be placed
 * @param y The y-coordinate where the content's center should be placed
 * @param content The composable content to center at the specified position
 */
@Composable
private fun LayoutCenteredAt(
    x: Dp,
    y: Dp,
    content: @Composable () -> Unit,
) = Layout(
    content = content,
    measurePolicy = { measurables, constraints ->
        val placeable = measurables.firstOrNull()?.measure(constraints)
        layout(constraints.maxWidth, constraints.maxHeight) {
            placeable?.placeRelative(
                x = x.roundToPx() - placeable.width / 2,
                y = y.roundToPx() - placeable.height / 2,
            )
        }
    },
)

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

@Composable
private fun DrawTable(modifier: Modifier = Modifier) {
    val pokerTable = painterResource(Res.drawable.pokerTable)
    Image(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        painter = pokerTable,
        contentDescription = "Poker Table",
        contentScale = ContentScale.FillBounds,
    )
}

@Composable
private fun ShowPlayer(
    modifier: Modifier = Modifier,
    player: PlayerState,
) {
    Box(
        modifier = modifier,
    ) {
        ShowHand(
            hand = player.dealtCards,
        )
        PlayerDetails(
            player = player,
        )
    }
}

@Composable
private fun BoxScope.PlayerDetails(
    modifier: Modifier = Modifier,
    player: PlayerState,
) {
    Box(
        modifier = modifier
            .align(Alignment.BottomCenter)
            .background(color = Color.White),
    ) {
        Text(
            modifier = Modifier.align(Alignment.BottomCenter),
            text = "${player.player.name}\n${player.chips}",
            textAlign = TextAlign.Center,
            style = TextStyle(fontSize = 16.sp),
            color = Color.Black,
        )
    }
}

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
private fun WagerChips(
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
private fun AnimatingChipStacks(
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ShowHand(
    modifier: Modifier = Modifier,
    hand: List<DealtCard>,
) {
    val windowInfo = LocalWindowInfo.current
    val windowSize = windowInfo.containerSize

    // Calculate card size based on window dimensions
    val cardHeight = minOf(windowSize.width, windowSize.height) * 0.125f

    Box(
        modifier = modifier,
    ) {
        hand.forEachIndexed { index, card ->
            DealtCardView(
                modifier = Modifier
                    .offset { IntOffset((index * 16), 0) }
                    .graphicsLayer {
                        rotationZ = index * 10f
                    }
                    .requiredHeight(cardHeight.dp),
                dealtCard = card,
            )
        }
    }
}

private const val DEALER_BUTTON_SIZE = 75

@Composable
private fun DealerButton(
    modifier: Modifier = Modifier,
    size: Dp = DEALER_BUTTON_SIZE.dp,
) {
    val gradient = Brush.radialGradient(
        colors = listOf(Color.LightGray, Color.White),
        radius = size.value / 2,
    )

    Box(
        modifier = modifier
            .size(size)
            .background(Color.LightGray, CircleShape)
            .padding(2.dp)
            .shadow(1.dp, CircleShape),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(brush = gradient)
        }
        Text(
            modifier = Modifier
                .graphicsLayer {
                    shadowElevation = 1.dp.toPx()
                    shape = RoundedCornerShape(4.dp)
                    clip = true
                }
                .align(Alignment.Center),
            text = "DEALER",
            color = Color.Black,
            style = TextStyle(
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = Color.White,
                    blurRadius = 1f,
                    offset = Offset(1f, 1f),
                ),
            ),
            fontSize = (size.value * 0.2f).sp,
            textAlign = TextAlign.Center,
        )
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

@Composable
private fun CommunityCards(
    modifier: Modifier = Modifier,
    communityCards: List<Card>,
) = Row(modifier = modifier) {
    communityCards.forEach { card ->
        PlayingCard(
            modifier = Modifier.requiredHeight(100.dp),
            card = card,
        )
    }
}

fun Double.toRadians(): Double = this / 180.0 * PI
