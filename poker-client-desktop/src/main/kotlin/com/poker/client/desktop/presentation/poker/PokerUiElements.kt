package com.poker.client.desktop.presentation.poker

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.poker.common.data.remote.dto.CardDto
import com.poker.common.data.remote.dto.PlayerDto

@Composable
internal fun ShowPlayers(
    modifier: Modifier = Modifier,
    players: List<PlayerDto>,
) {
    Column(
        modifier = modifier,
    ) {
        players.forEach { player ->
            player.hand?.let { hand -> ShowHand(hand = hand) }
            Text(player.hand.toString())
            Text(player.name)
        }
    }
}

@Composable
fun ShowHand(
    modifier: Modifier = Modifier,
    hand: List<CardDto>,
) {
    var imageHeight by remember { mutableStateOf(324) }
    Row(
        modifier = modifier,
    ) {
        hand.forEachIndexed { index, card ->
            ShowCard(
                modifier =
                    modifier
                        .then(
                            if (card == hand.last()) {
                                modifier
                                    .offset(x = (-index * ((120 / 324f) * imageHeight)).dp)
                                    // .width(((30 / 324f) * imageHeight).coerceAtLeast(30f).dp)
                                    .onGloballyPositioned { coordinates ->
                                        imageHeight = coordinates.size.height
                                    }
                            } else {
                                modifier
                            },
                        ),
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
    var imageHeight by remember { mutableStateOf(324) }
    Image(
        modifier = modifier,
        painter = cardDeck,
        contentDescription = card.toString(),
    )
}
