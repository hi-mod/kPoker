package com.poker.client.desktop.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import com.poker.client.desktop.di.AppModule

@Composable
fun FrameWindowScope.MainMenu(
    appModule: AppModule,
    onStartGame: () -> Unit = {},
    onExitApplication: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    MenuBar {
        Menu(
            text = "Poker",
        ) {
            Item(
                text = "Start Game",
                onClick = onStartGame,
            )
            Separator()
            Item(
                text = "Quit",
                onClick = onExitApplication,
            )
        }
    }
}