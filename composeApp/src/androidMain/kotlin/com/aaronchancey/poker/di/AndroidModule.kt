package com.aaronchancey.poker.di

import android.content.Context
import com.aaronchancey.poker.presentation.sound.SoundPlayer
import com.aaronchancey.poker.presentation.sound.SoundPlayerImpl
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.dsl.module

/**
 * Android-specific Koin module providing platform implementations.
 *
 * Provides:
 * - [Settings] via SharedPreferences
 * - [SoundPlayer] via Android audio implementation
 */
val androidModule = module {
    single<Settings> {
        SharedPreferencesSettings(
            get<Context>().getSharedPreferences("poker_prefs", Context.MODE_PRIVATE),
        )
    }
    single<SoundPlayer> { SoundPlayerImpl(get()) }
}
