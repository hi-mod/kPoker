package com.aaronchancey.poker.presentation.room.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaronchancey.poker.kpoker.core.Card
import kotlin.math.PI
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
fun LayoutCenteredAt(
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
fun DrawTable(modifier: Modifier = Modifier) {
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
fun CommunityCards(
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

private const val DEALER_BUTTON_SIZE = 75

@Composable
fun DealerButton(
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

fun Double.toRadians(): Double = this / 180.0 * PI

/**
 * Represents an ellipse geometry for positioning elements around the poker table.
 * Calculates positions at various distances from center (using scale factor).
 */
data class EllipseGeometry(
    val centerX: Dp,
    val centerY: Dp,
    val radiusX: Dp,
    val radiusY: Dp,
    val angleStep: Double,
) {
    /**
     * Calculates the (x, y) position on the ellipse for a given seat number.
     *
     * @param seatNumber The seat number (1-indexed)
     * @param scaleFactor Distance from center (1.0 = on the ellipse, 0.5 = halfway to center)
     * @return Pair of (x, y) Dp coordinates
     */
    fun positionForSeat(seatNumber: Int, scaleFactor: Float = 1f): Pair<Dp, Dp> {
        val angle = ((seatNumber - 1) * angleStep).toRadians()
        val x = centerX + radiusX * kotlin.math.cos(angle).toFloat() * scaleFactor
        val y = centerY + radiusY * kotlin.math.sin(angle).toFloat() * scaleFactor
        return x to y
    }
}
