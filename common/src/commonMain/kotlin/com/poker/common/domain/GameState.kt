package com.poker.common.domain

interface IGameState {
    val game: Game
}

sealed class GameState() {
    data object Idle : GameState()
    data class GameStart(override val game: Game) : GameState(), IGameState
    data class HandStart(override val game: Game) : GameState(), IGameState
    sealed class Street() : GameState(), IGameState {
        data class PreFlop(override val game: Game) : Street(), IGameState
        data class Flop(override val game: Game) : Street(), IGameState
        data class Turn(override val game: Game) : Street(), IGameState
        data class River(override val game: Game) : Street(), IGameState
    }
    data class Showdown(override val game: Game) : GameState(), IGameState
    data class HandComplete(override val game: Game) : GameState(), IGameState
    data object EndState : GameState()
}
