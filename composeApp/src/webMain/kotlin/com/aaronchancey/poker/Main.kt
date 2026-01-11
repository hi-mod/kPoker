package com.aaronchancey.poker

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import com.russhwolf.settings.StorageSettings

@OptIn(ExperimentalComposeUiApi::class)
fun main() = ComposeViewport {
    val settings = StorageSettings()
    App(settings = settings)
}
