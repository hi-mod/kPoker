package com.aaronchancey.poker.presentation.game.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aaronchancey.poker.kpoker.core.Card
import org.jetbrains.compose.resources.painterResource
import poker.composeapp.generated.resources.Res
import poker.composeapp.generated.resources.allDrawableResources

@Composable
fun PlayingCard(
    modifier: Modifier = Modifier,
    card: Card,
) {
    val cardString = "${if (card.rank.symbol.toIntOrNull() != null) "_" else ""}${card.rank.symbol}${card.suit.symbol}"
    val cardPainter = painterResource(Res.allDrawableResources[cardString]!!)
    Image(
        modifier = modifier,
        painter = cardPainter,
        contentDescription = card.toString(),
    )
}
