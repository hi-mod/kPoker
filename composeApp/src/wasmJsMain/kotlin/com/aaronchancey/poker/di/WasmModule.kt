package com.aaronchancey.poker.di

import com.aaronchancey.poker.presentation.sound.SoundPlayer
import com.aaronchancey.poker.presentation.sound.SoundPlayerImpl
import com.russhwolf.settings.Settings
import com.russhwolf.settings.StorageSettings
import org.koin.dsl.module

/**
 * WASM/JS-specific Koin module providing platform implementations.
 *
 * Provides:
 * - [Settings] via browser localStorage
 * - [SoundPlayer] via Web Audio API
 */
val wasmModule = module {
    single<Settings> { StorageSettings() }
    single<SoundPlayer> { SoundPlayerImpl() }
}
