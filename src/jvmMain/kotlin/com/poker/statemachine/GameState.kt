package com.poker.statemachine

import com.poker.domain.Game

sealed class GameState() {
    data object Idle : GameState()
    data class GameStart(val game: Game) : GameState()
    data class HandStart(val game: Game) : GameState()
    sealed class Street(
        open val game: Game,
        val nextGameState: (Game) -> GameState,
    ) : GameState() {
        data class PreFlop(override val game: Game) : Street(game, { Flop(it) })
        data class Flop(override val game: Game) : Street(game, { Turn(it) })
        data class Turn(override val game: Game) : Street(game, { River(it) })
        data class River(override val game: Game) : Street(game, { Showdown(it) })
    }
    data class Showdown(val game: Game) : GameState()
    data class HandComplete(val game: Game) : GameState()
    data object EndState : GameState()
}
