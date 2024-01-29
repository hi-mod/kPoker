package com.poker.client.desktop

import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.poker.client.desktop.di.AppModuleImpl
import com.poker.client.desktop.presentation.components.MainMenu
import com.poker.client.desktop.presentation.PokerScreen

fun main() = application {
    val windowState = rememberWindowState()

    val appModule =
        remember {
            AppModuleImpl()
        }

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Poker Desktop Client",
    ) {
        MainMenu(
            appModule = appModule,
            onStartGame = { appModule.pokerViewModel.startGame() },
            onExitApplication = ::exitApplication,
        )
        PokerScreen(appModule = appModule)
    }
}
