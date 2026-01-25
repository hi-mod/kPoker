package com.aaronchancey.poker.presentation.room.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aaronchancey.poker.kpoker.core.DealtCard
import com.aaronchancey.poker.kpoker.player.PlayerState

@Composable
fun ShowPlayer(
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
