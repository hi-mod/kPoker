package com.poker.common.data.remote

import com.poker.common.data.remote.dto.poker.GameDto
import kotlinx.coroutines.flow.Flow

interface PokerService {
    suspend fun startGame(gameId: String): Flow<GameDto>
}
