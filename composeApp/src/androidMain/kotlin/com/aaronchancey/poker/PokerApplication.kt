package com.aaronchancey.poker

import android.app.Application
import android.content.Context
import com.aaronchancey.poker.di.appModule
import com.russhwolf.settings.SharedPreferencesSettings
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class PokerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@PokerApplication)
            modules(
                appModule,
                module {
                    single<com.russhwolf.settings.Settings> {
                        SharedPreferencesSettings(
                            get<Context>().getSharedPreferences("poker_prefs", MODE_PRIVATE),
                        )
                    }
                },
            )
        }
    }
}
