package com.aaronchancey.poker

import androidx.compose.ui.window.ComposeUIViewController
import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults

fun mainViewController() = ComposeUIViewController {
    val settings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    App(settings = settings)
}
