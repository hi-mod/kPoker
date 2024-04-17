package com.poker.client.desktop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.poker.client.desktop.di.AppModuleImpl
import com.poker.client.desktop.presentation.PokerScreen
import com.poker.client.desktop.presentation.components.LoginDialog
import com.poker.client.desktop.presentation.components.MainMenu

fun main() = application {
    val windowState = rememberWindowState()

    val appModule =
        remember {
            AppModuleImpl()
        }

    val state by appModule.pokerViewModel.state.collectAsState()

    Window(
        onCloseRequest = ::exitApplication,
        state = windowState,
        title = "Poker Desktop Client",
    ) {
        MainMenu(
            onEvent = appModule.pokerViewModel::onEvent,
        )
        PokerScreen(appModule = appModule)

        if (state.showLogin) {
            LoginDialog(
                state = state,
                onEvent = appModule.pokerViewModel::onEvent,
            )
        }
    }

    if (state.exitApplication) {
        exitApplication()
    }
}
