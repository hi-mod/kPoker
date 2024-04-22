package com.poker.client.desktop.table.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import com.poker.client.desktop.table.presentation.PokerEvent

@Composable
fun FrameWindowScope.MainMenu(
    onEvent: (PokerEvent) -> Unit,
) {
    MenuBar {
        Menu(
            text = "Poker",
        ) {
            Item(
                text = "Login",
                onClick = { onEvent(PokerEvent.OnLoginMenuClick) },
            )
            Item(
                text = "Start Game",
                onClick = { onEvent(PokerEvent.StartGame) },
            )
            Separator()
            Item(
                text = "Quit",
                onClick = { onEvent(PokerEvent.ExitApplication) },
            )
        }
    }
}
