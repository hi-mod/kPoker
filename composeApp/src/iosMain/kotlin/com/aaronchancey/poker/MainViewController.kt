package com.aaronchancey.poker

import androidx.compose.ui.window.ComposeUIViewController
import com.aaronchancey.poker.di.appModule
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.compose.KoinApplication
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

fun mainViewController() = ComposeUIViewController {
    val settings = NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults)
    KoinApplication(application = {
        modules(appModule, module { single<Settings> { settings } })
    }) {
        App()
    }
}
