package com.aaronchancey.poker.presentation.room

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aaronchancey.poker.di.AppModule
import com.aaronchancey.poker.domain.onSuccess
import com.aaronchancey.poker.network.RoomClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class RoomViewModel(
    private val roomClient: RoomClient = AppModule.roomClient,
) : ViewModel() {
    private val _state = MutableStateFlow(RoomState())
    val state = _state
        .asStateFlow()
        .onStart {
            _state.update { it.copy(isLoading = true) }
            roomClient.getRooms()
                .onSuccess { rooms ->
                    _state.update {
                        it.copy(rooms = rooms)
                    }
                }
            _state.update { it.copy(isLoading = false) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RoomState())
}
