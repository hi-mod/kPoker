package com.aaronchancey.poker.presentation.room.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
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
import com.aaronchancey.poker.kpoker.player.PlayerStatus

/**
 * Displays an occupied seat with the player's cards and nameplate.
 *
 * @param player The player state to display
 */
@Composable
internal fun OccupiedSeat(
    modifier: Modifier = Modifier,
    currentActor: PlayerState?,
    player: PlayerState,
) {
    Box(modifier = modifier) {
        HoleCards(hand = player.dealtCards)
        PlayerNameplate(currentActor = currentActor, player = player)
    }
}

/**
 * Displays the player's name and chip count at the bottom of the seat.
 */
@Composable
private fun BoxScope.PlayerNameplate(
    currentActor: PlayerState?,
    player: PlayerState,
) = Box(
    modifier = Modifier
        .align(Alignment.BottomCenter)
        .background(color = Color.White)
        .then(if (currentActor?.player?.id == player.player.id) Modifier.border(width = 2.dp, color = Color.Red) else Modifier)
        .padding(4.dp),
) {
    val statusSuffix = if (player.status == PlayerStatus.SITTING_OUT) "\nSITTING OUT" else ""
    Text(
        modifier = Modifier.align(Alignment.BottomCenter),
        text = "${player.player.name}\n${player.chips}$statusSuffix",
        textAlign = TextAlign.Center,
        style = TextStyle(fontSize = 16.sp),
        color = if (player.status == PlayerStatus.SITTING_OUT) Color.Gray else Color.Black,
    )
}

/**
 * Displays the player's hole cards with a slight fan effect.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun HoleCards(hand: List<DealtCard>) {
    val windowInfo = LocalWindowInfo.current
    val windowSize = windowInfo.containerSize
    val cardHeight = remember(windowSize) {
        minOf(windowSize.width, windowSize.height) * 0.125f
    }

    Box {
        hand.forEachIndexed { index, card ->
            key(index) {
                DealtCardView(
                    modifier = Modifier
                        .offset { IntOffset(x = index * 8, y = 0) }
                        .graphicsLayer { rotationZ = index * 10f }
                        .requiredHeight(cardHeight.dp),
                    dealtCard = card,
                )
            }
        }
    }
}
