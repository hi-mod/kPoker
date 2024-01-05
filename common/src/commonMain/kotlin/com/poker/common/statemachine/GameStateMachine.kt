package com.poker.common.statemachine

import com.poker.common.domain.Card
import com.poker.common.domain.CardRank
import com.poker.common.domain.CardSuit
import com.poker.common.domain.Deck
import com.poker.common.domain.Game
import com.poker.common.domain.GameEvent
import com.poker.common.domain.GameState
import com.poker.common.domain.PokerAction
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.ExperimentalSerializationApi

@ExperimentalSerializationApi
class GameStateMachine(
    var gameState: GameState = GameState.Idle,
) {
    fun stateMachine(inputChannel: Channel<GameEvent>) = flow {
        var currentState = gameState
        while (true) {
            emit(currentState)
            val gameEvent = inputChannel.receive()
            currentState = nextState(currentState, gameEvent)
            gameState = currentState
        }
    }//.distinctUntilChanged()

    private fun nextState(
        currentState: GameState,
        gameEvent: GameEvent,
    ): GameState = when (currentState) {
        GameState.Idle -> idleGameStateEvents(gameEvent, currentState)
        is GameState.GameStart -> gameStartGameStateEvents(gameEvent, currentState)
        is GameState.Street.PreFlop -> gameStreetGameStateEvents(
            gameEvent = gameEvent,
            nextGameState = { GameState.Street.Flop(it) },
            currentState = currentState
        ) { GameState.Street.PreFlop(it) }
        is GameState.Street.Flop -> gameStreetGameStateEvents(
            gameEvent = gameEvent,
            nextGameState = { GameState.Street.Turn(it) },
            currentState = currentState
        ) { GameState.Street.Flop(it) }
        is GameState.Street.Turn -> gameStreetGameStateEvents(
            gameEvent = gameEvent,
            nextGameState = { GameState.Street.River(it) },
            currentState = currentState
        ) { GameState.Street.Turn(it) }
        is GameState.Street.River -> gameStreetGameStateEvents(
            gameEvent = gameEvent,
            nextGameState = { GameState.Showdown(it) },
            currentState = currentState
        ) { GameState.Street.River(it) }
        is GameState.Showdown -> gameShowdownGameStateEvents(gameEvent, currentState)
        is GameState.HandComplete -> gameHandCompleteStateEvents(gameEvent, currentState)
        else -> currentState
    }

    private fun gameHandCompleteStateEvents(
        gameEvent: GameEvent,
        currentState: GameState.HandComplete
    ): GameState = when (gameEvent) {
        is GameEvent.StartHand -> GameState.HandStart(currentState.game.startHand())
        else -> currentState
    }

    private fun gameShowdownGameStateEvents(
        gameEvent: GameEvent,
        currentState: GameState.Showdown,
    ): GameState = when (gameEvent) {
        is GameEvent.AwardPot -> GameState.Showdown(currentState.game.awardPot())
        is GameEvent.EndHand -> GameState.HandComplete(currentState.game.endHand())
        else -> currentState
    }

    private fun gameStreetGameStateEvents(
        gameEvent: GameEvent,
        nextGameState: (Game) -> GameState,
        currentState: GameState.Street,
        sameState: (Game) -> GameState,
    ) = when (gameEvent) {
        is GameEvent.AddViewer -> sameState(currentState.game.addViewer(gameEvent.userId))
        is GameEvent.SelectPlayerAction -> {
            val game = currentState.game.performPlayerAction(gameEvent)
            if (game.allPlayersHaveActed()) {
                nextGameState(game)
            } else {
                sameState(game)
            }
        }
        else -> currentState
    }

    private fun gameStartGameStateEvents(
        gameEvent: GameEvent,
        currentState: GameState.GameStart
    ): GameState = when (gameEvent) {
        is GameEvent.AddPlayer -> {
            val game = currentState.game.addPlayer(gameEvent.player)
            GameState.GameStart(game)
        }
        is GameEvent.RemovePlayer -> GameState.GameStart(currentState.game.removePlayer(gameEvent.player))
        is GameEvent.AddViewer -> GameState.GameStart(currentState.game.addViewer(gameEvent.userId))
        GameEvent.SetButton -> GameState.GameStart(setButton(currentState.game))
        GameEvent.ChooseStartingDealer -> chooseStartingDealer(currentState)
        GameEvent.GameReady -> {
            val game = currentState.game
            if (game.players.size >= game.minPlayers) {
                GameState.Street.PreFlop(game.startHand())
            } else {
                currentState
            }
        }
        else -> currentState
    }

    private fun idleGameStateEvents(
        gameEvent: GameEvent,
        currentState: GameState
    ) = if (gameEvent is GameEvent.StartGame) {
        GameState.GameStart(gameEvent.game)
    } else {
        currentState
    }

    private fun Game.startHand(): Game {
        deck.shuffle()
        val gameAfterDealingCards = copy(
            activePlayer = null,
            handNumber = handNumber.inc(),
            players = players.map { player ->
                player.copy(
                    hand = emptyList(),
                    currentWager = 0.0,
                    hasActed = false,
                    hasFolded = false,
                )
            },
        )
            .postBlindsAndAntes()
            .dealCards()

        val gameAfterSettingActions = gameAfterDealingCards.copy(
            players = gameAfterDealingCards.players.mapIndexed { index, player ->
                player.copy(
                    availablePlayerActions = if(index == 0) {
                        listOf(PokerAction.Call, PokerAction.Fold, PokerAction.Raise)
                    } else {
                        emptyList()
                    },
                )
            }
        )

        return if(gameAfterSettingActions.handNumber != 1L) {
            gameAfterSettingActions.moveButton()
        } else {
            gameAfterSettingActions
        }
    }

    private fun Game.moveButton(): Game {
        val smallBindPlayer = players.first()
        return copy(
            buttonPosition = if (buttonPosition == players.size) 1 else buttonPosition.inc(),
            players = players.filter { it != smallBindPlayer } + smallBindPlayer,
        )
    }

    private fun Game.postBlindsAndAntes() = level.let { level ->
        val ante = (level.ante ?: 0.0)
        players.mapIndexed { index, player ->
            when (index) {
                0 -> players[index].wager(level.smallBlind + ante).copy(hasActed = false)
                1 -> players[index].wager(level.bigBlind + ante).copy(hasActed = false)
                else -> player
            }
        }
    }.let { players -> copy(players = players, pot = players.sumOf { it.currentWager }) }

    private fun chooseStartingDealer(currentState: GameState.GameStart): GameState.GameStart {
        val deck = currentState.game.deck
        deck.shuffle()
        val cards = mutableListOf<Card>()
        for (i in 1..currentState.game.players.size) {
            cards.add(deck.popCard())
            if (cards.last() == Card(CardRank.Ace, CardSuit.Spades)) break
        }
        return if (cards.size < 10) {
            GameState.GameStart(
                currentState.game.copy(
                    buttonPosition = cards.size,
                    players = currentState.game.players.mapIndexed { i, player ->
                        player.copy(
                            hand = listOf(cards[i]),
                        )
                    },
                )
            )
        } else {
            GameState.GameStart(
                currentState.game.copy(
                    buttonPosition = cards
                        .withIndex()
                        .maxWithOrNull(
                            compareBy(
                                compareBy<Card> { it.rank.value }
                                    .thenBy { it.suit.value },
                            ) { it.value },
                        )
                        ?.index?.plus(1) ?: 0,
                    players = currentState.game.players.mapIndexed { i, player ->
                        player.copy(
                            hand = listOf(cards[i]),
                        )
                    },
                ),
            )
        }
    }

    private fun setButton(game: Game): Game {
        val buttonPlayerIndex = game.buttonPosition - 1
        val smallBlindPlayerIndex = if (buttonPlayerIndex >= game.players.size - 1) 0 else buttonPlayerIndex + 1
        val bigBlindPlayerIndex = when {
            buttonPlayerIndex >= game.players.size - 1 -> 1
            buttonPlayerIndex + 1 >= game.players.size - 1 -> 0
            else -> buttonPlayerIndex + 1
        }
        val buttonPlayer = game.players[buttonPlayerIndex]
        val smallBlindPlayer = game.players[if (game.players.size == 2) buttonPlayerIndex else smallBlindPlayerIndex]
        val bigBlindPlayer = game.players[if (game.players.size == 2) smallBlindPlayerIndex else bigBlindPlayerIndex]
        val buttonAndBlindsPlayers = if (game.players.size == 2) {
            listOf(
                smallBlindPlayer,
                bigBlindPlayer,
            )
        } else {
            listOf(
                buttonPlayer,
                smallBlindPlayer,
                bigBlindPlayer,
            )
        }
        val players = if (game.players.size == 2) {
            buttonAndBlindsPlayers
        } else {
            game.players.filter { it == smallBlindPlayer }
                .plus(game.players.filter { it == bigBlindPlayer })
                .plus(game.players.filter { !buttonAndBlindsPlayers.contains(it) })
                .plus(game.players.filter { it == buttonPlayer })
        }
        return game.copy(players = players)
    }
}
