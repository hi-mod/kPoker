package com.aaronchancey.poker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingWindow
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.russhwolf.settings.PreferencesSettings
import java.util.prefs.Preferences

@OptIn(ExperimentalComposeUiApi::class)
fun main() = application {
    val preferences = Preferences.userRoot().node("com.aaronchancey.poker")
    val settings = PreferencesSettings(preferences)

    val windowState = rememberWindowState()
    SwingWindow(
        onCloseRequest = ::exitApplication,
        state = windowState,
        init = {},
    ) {
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
        ) {
            App(settings = settings)
        }
    }
}
