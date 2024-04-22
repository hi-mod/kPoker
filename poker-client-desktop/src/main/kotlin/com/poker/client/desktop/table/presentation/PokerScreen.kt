package com.poker.client.desktop.table.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.poker.client.desktop.di.AppModule
import com.poker.client.desktop.table.presentation.components.ShowPlayers

@Composable
fun PokerScreen(appModule: AppModule) {
    val state by appModule.pokerViewModel.state.collectAsState()
    ShowPlayers(players = state.players)
}
