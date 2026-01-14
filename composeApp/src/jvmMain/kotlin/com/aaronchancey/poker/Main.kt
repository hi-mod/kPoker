package com.aaronchancey.poker

import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
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
        val scrollState = rememberScrollState()
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
        ) {
            Box(modifier = Modifier.verticalScroll(scrollState)) {
                App(settings = settings)
            }
            VerticalScrollbar(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState),
            )
        }
    }
}
