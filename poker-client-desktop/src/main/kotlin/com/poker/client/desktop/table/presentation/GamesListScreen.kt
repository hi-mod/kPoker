package com.poker.client.desktop.table.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.poker.common.domain.Game

@Composable
fun GamesListScreen(availableGames: List<Game>) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(all = 16.dp),
        ) {
            GameList(availableGames)
            Button(
                onClick = { /*TODO*/ },
                modifier = Modifier
                    .fillMaxWidth(),
            ) {
                Text("Start Game")
            }
        }
    }
}

@Composable
private fun GameList(
    availableGames: List<Game>,
) {
    var selectedGame by remember { mutableStateOf<Game?>(null) }
    LazyColumn {
        item {
            Title()
        }
        item {
            GameHeaders()
        }
        items(availableGames) { game ->
            GameEntries(
                game = game,
                selectedGame = selectedGame,
                onGameSelected = { selectedGame = it },
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun GameEntries(
    game: Game,
    selectedGame: Game?,
    onGameSelected: (Game) -> Unit,
) {
    var isHovered by remember { mutableStateOf(false) }
    val isSelected = selectedGame == game
    val foregroundColor = if (isSelected) Color.White else Color.Black
    val backgroundColor = when {
        isSelected -> Color(0xff0078d7)
        isHovered -> Color.LightGray
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(vertical = 8.dp)
            .clickable { onGameSelected(game) }
            .onPointerEvent(PointerEventType.Enter) {
                isHovered = true
            }
            .onPointerEvent(PointerEventType.Exit) {
                isHovered = false
            },
    ) {
        Text(
            text = game.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = foregroundColor,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        Text(
            text = game.description,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = foregroundColor,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        Text(
            text = game.numPlayers.toString(),
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = foregroundColor,
            modifier = Modifier
                .weight(.5f)
                .padding(horizontal = 8.dp),
        )
        Text(
            text = "${game.level.smallBlind}/${game.level.bigBlind}",
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal,
            color = foregroundColor,
            modifier = Modifier
                .weight(.5f)
                .padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun Title() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Games List",
            fontWeight = FontWeight.ExtraBold,
            fontSize = 24.sp,
        )
    }
}

@Composable
private fun GameHeaders() {
    Row(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = "Name",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        Text(
            text = "Description",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        )
        Text(
            text = "# Players",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .weight(.5f)
                .padding(horizontal = 8.dp),
        )
        Text(
            text = "Level",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .weight(.5f)
                .padding(horizontal = 8.dp),
        )
    }
}
