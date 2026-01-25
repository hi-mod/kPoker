package com.aaronchancey.poker.di

import com.aaronchancey.poker.presentation.sound.SoundPlayer
import com.aaronchancey.poker.presentation.sound.SoundPlayerImpl
import com.russhwolf.settings.PreferencesSettings
import com.russhwolf.settings.Settings
import java.util.prefs.Preferences
import org.koin.dsl.module

/**
 * JVM-specific Koin module providing platform implementations.
 *
 * Provides:
 * - [Settings] via Java Preferences API
 * - [SoundPlayer] via JVM audio implementation
 */
val jvmModule = module {
    single<Settings> {
        val preferences = Preferences.userRoot().node("com.aaronchancey.poker")
        PreferencesSettings(preferences)
    }
    single<SoundPlayer> { SoundPlayerImpl() }
}
