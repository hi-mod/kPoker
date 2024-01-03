package com.poker.server.statemachine

import com.poker.server.domain.Game
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable

interface IGameState {
    @ExperimentalSerializationApi
    val game: Game
}

@ExperimentalSerializationApi
@Serializable
sealed class GameState() {
    data object Idle : GameState()
    @Serializable
    data class GameStart(override val game: Game) : GameState(), IGameState
    data class HandStart(override val game: Game) : GameState(), IGameState
    @Serializable
    sealed class Street() : GameState(), IGameState {
        @Serializable
        data class PreFlop(override val game: Game) : Street(), IGameState
        data class Flop(override val game: Game) : Street(), IGameState
        data class Turn(override val game: Game) : Street(), IGameState
        data class River(override val game: Game) : Street(), IGameState
    }
    data class Showdown(override val game: Game) : GameState(), IGameState
    data class HandComplete(override val game: Game) : GameState(), IGameState
    data object EndState : GameState()
}
