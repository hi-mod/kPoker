package com.poker.common.domain

import kotlinx.serialization.Serializable

sealed class GameEvent {
    data class AddPlayer(val player: Player) : GameEvent()
    data class AddViewer(val userId: String) : GameEvent()
    data class RemovePlayer(val player: Player) : GameEvent()

    @Serializable
    sealed class SelectPlayerAction(open val amount: Double = 0.0) : GameEvent() {
        data class Bet(override val amount: Double) : SelectPlayerAction(amount)
        data object Call : SelectPlayerAction()
        data object Check : SelectPlayerAction()
        data object Fold : SelectPlayerAction()
        data class Raise(override val amount: Double) : SelectPlayerAction(amount)
    }
    data class StartGame(val game: Game) : GameEvent()
    data object GameReady : GameEvent()
    data object ChooseStartingDealer : GameEvent()
    data object AwardPot : GameEvent()
    data object EndGame : GameEvent()
    data object EndHand : GameEvent()
    data object SetButton : GameEvent()
    data object StartHand : GameEvent()
}
