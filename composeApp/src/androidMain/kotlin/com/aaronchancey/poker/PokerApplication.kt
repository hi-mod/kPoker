package com.aaronchancey.poker

import android.app.Application
import com.aaronchancey.poker.di.androidModule
import com.aaronchancey.poker.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class PokerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@PokerApplication)
            modules(appModule, androidModule)
        }
    }
}
