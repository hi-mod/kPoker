package com.aaronchancey.poker.di

import com.aaronchancey.poker.presentation.sound.SoundPlayer
import com.aaronchancey.poker.presentation.sound.SoundPlayerImpl
import com.russhwolf.settings.NSUserDefaultsSettings
import com.russhwolf.settings.Settings
import org.koin.dsl.module
import platform.Foundation.NSUserDefaults

/**
 * iOS-specific Koin module providing platform implementations.
 *
 * Provides:
 * - [Settings] via NSUserDefaults
 * - [SoundPlayer] via AVFoundation
 */
val iosModule = module {
    single<Settings> { NSUserDefaultsSettings(NSUserDefaults.standardUserDefaults) }
    single<SoundPlayer> { SoundPlayerImpl() }
}
