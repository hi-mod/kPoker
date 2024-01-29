package com.poker.client.desktop.di

import com.poker.client.desktop.presentation.PokerViewModel
import com.poker.common.data.remote.PokerService
import kotlinx.coroutines.MainScope

interface AppModule {
    val pokerViewModel: PokerViewModel
}

class AppModuleImpl : AppModule {
    private val pokerService by lazy {
        PokerService.create()
    }

    override val pokerViewModel by lazy {
        PokerViewModel(
            coroutineScope =  MainScope(),
            pokerService = pokerService,
        )
    }
}
