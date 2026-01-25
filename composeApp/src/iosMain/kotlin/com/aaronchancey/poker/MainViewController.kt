package com.aaronchancey.poker

import androidx.compose.ui.window.ComposeUIViewController
import com.aaronchancey.poker.di.appModule
import com.aaronchancey.poker.di.iosModule
import org.koin.compose.KoinApplication
import org.koin.dsl.koinConfiguration

fun mainViewController() = ComposeUIViewController {
    KoinApplication(
        configuration = koinConfiguration(
            declaration = {
                modules(appModule, iosModule)
            },
        ),
        content = {
            App()
        },
    )
}
