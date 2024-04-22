package com.poker.common.domain

interface IGameState {
    val table: Table
}

sealed class GameState() {
    data object Idle : GameState()
    data class GameStart(override val table: Table) : GameState(), IGameState
    data class HandStart(override val table: Table) : GameState(), IGameState
    sealed class Street() : GameState(), IGameState {
        data class PreFlop(override val table: Table) : Street(), IGameState
        data class Flop(override val table: Table) : Street(), IGameState
        data class Turn(override val table: Table) : Street(), IGameState
        data class River(override val table: Table) : Street(), IGameState
    }
    data class Showdown(override val table: Table) : GameState(), IGameState
    data class HandComplete(override val table: Table) : GameState(), IGameState
    data object EndState : GameState()
}
