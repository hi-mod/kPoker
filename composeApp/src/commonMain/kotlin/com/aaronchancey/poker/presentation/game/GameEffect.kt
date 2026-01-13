package com.aaronchancey.poker.presentation.game

// Side Effects (One-time events)
sealed interface GameEffect {
    data class ShowToast(val message: String) : GameEffect
    data object NavigateToLobby : GameEffect
    data class PlaySound(val soundType: SoundType) : GameEffect
}
