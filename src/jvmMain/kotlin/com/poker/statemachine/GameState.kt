package com.poker.statemachine

import com.poker.domain.Game
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
    sealed class Street(
        override val game: Game,
        val nextGameState: (Game) -> GameState,
    ) : GameState(), IGameState {
        data class PreFlop(override val game: Game) : Street(game, { Flop(it) })
        data class Flop(override val game: Game) : Street(game, { Turn(it) })
        data class Turn(override val game: Game) : Street(game, { River(it) })
        data class River(override val game: Game) : Street(game, { Showdown(it) })
    }
    data class Showdown(override val game: Game) : GameState(), IGameState
    data class HandComplete(override val game: Game) : GameState(), IGameState
    data object EndState : GameState()
}
