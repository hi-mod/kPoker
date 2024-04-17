package com.poker.client.desktop.presentation

sealed interface PokerEvent {
    data object OnLoginMenuClick : PokerEvent
    data class SaveLogin(val user: String, val password: String) : PokerEvent
    data object StartGame : PokerEvent
    data object ExitApplication : PokerEvent
    data object OnDismissLogin : PokerEvent
    data object OnLogin : PokerEvent
}
