package com.aaronchancey.poker

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    val windowState = rememberWindowState()
    SwingWindow(
        onCloseRequest = ::exitApplication,
        state = windowState,
        init = {},
    ) {
        App()
    }
}
