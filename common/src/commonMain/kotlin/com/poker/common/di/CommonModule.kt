package com.poker.common.di

import com.poker.common.remote.PokerService

interface CommonModule {
    val pokerService: PokerService
}

class CommonModuleImpl : CommonModule {
    override val pokerService by lazy {
        PokerService.create()
    }
}
