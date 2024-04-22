package com.poker.common.domain

import com.poker.common.data.remote.dto.game.GameDto
import com.poker.common.data.remote.dto.poker.TableDto
import kotlinx.coroutines.flow.Flow

interface GameService {
    suspend fun getGames(): List<GameDto>
    suspend fun startGame(gameId: String): Flow<TableDto>
}
