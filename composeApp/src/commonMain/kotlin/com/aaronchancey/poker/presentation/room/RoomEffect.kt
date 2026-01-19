package com.aaronchancey.poker.presentation.room

sealed interface RoomEffect {
    data class ShowToast(val message: String) : RoomEffect
    data object NavigateToLobby : RoomEffect
    data class PlaySound(val soundType: SoundType) : RoomEffect
}
