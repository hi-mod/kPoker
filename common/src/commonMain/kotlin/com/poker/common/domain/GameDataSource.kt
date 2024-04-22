package com.poker.common.domain

import com.poker.common.core.Resource
import com.poker.common.data.mappers.toGame

class GameDataSource(
    private val gameService: GameService,
) {
    suspend fun getGames(): Resource<List<Game>> = try {
        Resource.Success(gameService.getGames().map { it.toGame() })
    } catch (e: Exception) {
        e.printStackTrace()
        Resource.Error(e.message ?: "An error occurred", null)
    }

    suspend fun startGame(gameId: String) = gameService.startGame(gameId)
}
