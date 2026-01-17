package com.aaronchancey.poker.presentation.game.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aaronchancey.poker.kpoker.core.Card
import com.aaronchancey.poker.kpoker.core.DealtCard
import org.jetbrains.compose.resources.painterResource
import poker.composeapp.generated.resources.Res
import poker.composeapp.generated.resources.allDrawableResources
import poker.composeapp.generated.resources.cardBackBlue

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

/**
 * Renders the back of a playing card (face-down).
 * Used when a card's value is hidden from the viewer.
 */
@Composable
fun CardBack(modifier: Modifier = Modifier) {
    Image(
        modifier = modifier,
        painter = painterResource(Res.drawable.cardBackBlue),
        contentDescription = "Card back",
    )
}

/**
 * Renders a dealt card, showing either the card face or a card back
 * depending on whether the card's value is visible to the viewer.
 *
 * @param dealtCard The dealt card with visibility information.
 *        If [DealtCard.card] is non-null, shows the card face.
 *        If null, shows a card back placeholder.
 */
@Composable
fun DealtCardView(
    modifier: Modifier = Modifier,
    dealtCard: DealtCard,
) {
    val card = dealtCard.card
    if (card != null) {
        PlayingCard(modifier = modifier, card = card)
    } else {
        CardBack(modifier = modifier)
    }
}
