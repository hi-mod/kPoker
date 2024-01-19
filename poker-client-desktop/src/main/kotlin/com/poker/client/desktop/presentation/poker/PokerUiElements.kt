package com.poker.client.desktop.presentation.poker

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poker.common.data.remote.dto.CardDto
import com.poker.common.data.remote.dto.PlayerDto
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private const val DealerButtonSize = 75

@Composable
fun ShowPlayers(
    modifier: Modifier = Modifier,
    players: List<PlayerDto>,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(color = Color.Green),
    ) {
        val pokerTable = painterResource("poker-table.svg")
        Image(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            painter = pokerTable,
            contentDescription = "Poker Table",
            contentScale = ContentScale.Fit,
        )
        val tableWidth = maxWidth * 0.85f
        val tableHeight = maxHeight * 0.65f
        val centerX = maxWidth * 0.5f
        val centerY = maxHeight * 0.5f
        val radiusX = tableWidth * 0.5f
        val radiusY = tableHeight * 0.5f

        val playerCount = players.size
        val angleStep = 360f / playerCount
        var currentAngle = 0f

        players.forEach { player ->
            val playerX = centerX + radiusX * cos(Math.toRadians(currentAngle.toDouble())).toFloat()
            val playerY = centerY + radiusY * sin(Math.toRadians(currentAngle.toDouble())).toFloat()

            println("${player.name} x: $playerX, y: $playerY")

            Layout(
                content = {
                    ShowPlayer(
                        player = player,
                        modifier = Modifier.offset(
                            x = playerX,
                            y = playerY,
                        )
                    )
                },
                measurePolicy = { measurables, constraints ->
                    val placeable = measurables.first().measure(constraints)
        
                    layout(placeable.width, placeable.height) {
                        placeable.placeRelative(
                            x = -placeable.width / 2,
                            y = -placeable.height / 2
                        )
                    }
                }
            )
        
            if(player == players.last()) {
                val dealerButtonFactorX = 0.6f // Adjust this value to move the dealer button
                val dealerButtonFactorY = 0.8f // Adjust this value to move the dealer button
                val dealerButtonX = centerX + radiusX * cos(Math.toRadians(currentAngle.toDouble())).toFloat() * dealerButtonFactorX
                val dealerButtonY = centerY + radiusY * sin(Math.toRadians(currentAngle.toDouble())).toFloat() * dealerButtonFactorY
                DealerButton(
                    modifier = Modifier
                        .offset(
                            x = dealerButtonX,
                            y = dealerButtonY,
                        ),
                )
            }

            currentAngle += angleStep
        }
    }
}

@Composable
private fun ShowPlayer(
    modifier: Modifier = Modifier,
    player: PlayerDto,
) {
    Box(
       modifier = modifier,
    ) {
        player.hand?.let { hand ->
            ShowHand(
                hand = hand,
            )
        }
        PlayerDetails(
            player = player,
        )
    }
}

@Composable
private fun BoxScope.PlayerDetails(
    modifier: Modifier = Modifier,
    player: PlayerDto,
) {
    Box(
        modifier = modifier
            .align(Alignment.BottomCenter)
            .background(color = Color.White),
    ) {
        Text(
            modifier = Modifier.align(Alignment.BottomCenter),
            text = "${player.name}\n${player.chips}",
            textAlign = TextAlign.Center,
            style = TextStyle(fontSize = 16.sp),
            color = Color.Black,
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun ShowHand(
    modifier: Modifier = Modifier,
    hand: List<CardDto>,
) {
    val windowInfo = LocalWindowInfo.current
    val windowSize = windowInfo.containerSize

    // Calculate card size based on window dimensions
    val cardHeight = min(windowSize.width, windowSize.height) * 0.125f

    Box(
        modifier = modifier
    ) {
        hand.forEachIndexed { index, card ->
            ShowCard(
                modifier = Modifier
                    .offset { IntOffset((index * 16), 0) }
                    .graphicsLayer {
                        rotationZ = index * 10f
                    }
                    .requiredHeight(cardHeight.dp),
                card = card,
            )
        }
    }
}

@Composable
private fun ShowCard(
    modifier: Modifier = Modifier,
    card: CardDto,
) {
     val cardDeck = painterResource("deck/${card.rank.cardName}${card.suit.shortName}.svg")
     Image(
         modifier = modifier,
         painter = cardDeck,
         contentDescription = card.toString(),
     )
}

@Composable
private fun DealerButton(
    modifier: Modifier = Modifier,
    size: Dp = DealerButtonSize.dp,
) {
    val gradient = Brush.radialGradient(
        colors = listOf(Color.LightGray, Color.White),
        radius = size.value / 2
    )

    Box(
        modifier = modifier
            .size(size)
            .background(Color.LightGray, CircleShape)
            .padding(2.dp)
            .shadow(1.dp, CircleShape)
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
                shadow = Shadow(color = Color.White, blurRadius = 1f, offset = Offset(1f, 1f))
            ),
            textAlign = TextAlign.Center,
        )
    }
}