package com.poker.common.statemachine

import com.poker.common.domain.Card
import com.poker.common.domain.CardRank
import com.poker.common.domain.CardSuit
import com.poker.common.domain.GameEvent
import com.poker.common.domain.GameState
import com.poker.common.domain.PokerAction
import com.poker.common.domain.Table
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
    } // .distinctUntilChanged()

    private fun nextState(
        currentState: GameState,
        gameEvent: GameEvent,
    ): GameState = when (currentState) {
        GameState.Idle -> idleGameStateEvents(gameEvent, currentState)
        is GameState.GameStart -> gameStartGameStateEvents(gameEvent, currentState)
        is GameState.Street.PreFlop -> gameStreetGameStateEvents(
            gameEvent = gameEvent,
            nextGameState = { GameState.Street.Flop(it) },
            currentState = currentState,
        ) { GameState.Street.PreFlop(it) }

        is GameState.Street.Flop -> gameStreetGameStateEvents(
            gameEvent = gameEvent,
            nextGameState = { GameState.Street.Turn(it) },
            currentState = currentState,
        ) { GameState.Street.Flop(it) }

        is GameState.Street.Turn -> gameStreetGameStateEvents(
            gameEvent = gameEvent,
            nextGameState = { GameState.Street.River(it) },
            currentState = currentState,
        ) { GameState.Street.Turn(it) }

        is GameState.Street.River -> gameStreetGameStateEvents(
            gameEvent = gameEvent,
            nextGameState = { GameState.Showdown(it) },
            currentState = currentState,
        ) { GameState.Street.River(it) }

        is GameState.Showdown -> gameShowdownGameStateEvents(gameEvent, currentState)
        is GameState.HandComplete -> gameHandCompleteStateEvents(gameEvent, currentState)
        else -> currentState
    }

    private fun gameHandCompleteStateEvents(
        gameEvent: GameEvent,
        currentState: GameState.HandComplete,
    ): GameState = when (gameEvent) {
        is GameEvent.StartHand -> GameState.HandStart(currentState.table.startHand())
        else -> currentState
    }

    private fun gameShowdownGameStateEvents(
        gameEvent: GameEvent,
        currentState: GameState.Showdown,
    ): GameState = when (gameEvent) {
        is GameEvent.AwardPot -> GameState.Showdown(currentState.table.awardPot())
        is GameEvent.EndHand -> GameState.HandComplete(currentState.table.endHand())
        else -> currentState
    }

    private fun gameStreetGameStateEvents(
        gameEvent: GameEvent,
        nextGameState: (Table) -> GameState,
        currentState: GameState.Street,
        sameState: (Table) -> GameState,
    ) = when (gameEvent) {
        is GameEvent.AddViewer -> sameState(currentState.table.addViewer(gameEvent.userId))
        is GameEvent.SelectPlayerAction -> {
            val game = currentState.table.performPlayerAction(gameEvent)
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
        currentState: GameState.GameStart,
    ): GameState = when (gameEvent) {
        is GameEvent.AddPlayer -> {
            val game = currentState.table.addPlayer(gameEvent.player)
            GameState.GameStart(game)
        }

        is GameEvent.RemovePlayer -> GameState.GameStart(currentState.table.removePlayer(gameEvent.player))
        is GameEvent.AddViewer -> GameState.GameStart(currentState.table.addViewer(gameEvent.userId))
        GameEvent.SetButton -> GameState.GameStart(setButton(currentState.table))
        GameEvent.ChooseStartingDealer -> chooseStartingDealer(currentState)
        GameEvent.GameReady -> {
            val game = currentState.table
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
        currentState: GameState,
    ) = if (gameEvent is GameEvent.StartGame) {
        GameState.GameStart(gameEvent.table)
    } else {
        currentState
    }

    private fun Table.startHand(): Table {
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
                    availablePlayerActions = if (index == 0) {
                        listOf(PokerAction.Call, PokerAction.Fold, PokerAction.Raise)
                    } else {
                        emptyList()
                    },
                )
            },
        )

        return if (gameAfterSettingActions.handNumber != 1L) {
            gameAfterSettingActions.moveButton()
        } else {
            gameAfterSettingActions
        }
    }

    private fun Table.moveButton(): Table {
        val smallBindPlayer = players.first()
        return copy(
            buttonPosition = if (buttonPosition == players.size) 1 else buttonPosition.inc(),
            players = players.filter { it != smallBindPlayer } + smallBindPlayer,
        )
    }

    private fun Table.postBlindsAndAntes() = level.let { level ->
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
        val deck = currentState.table.deck
        deck.shuffle()
        val cards = mutableListOf<Card>()
        for (i in 1..currentState.table.players.size) {
            cards.add(deck.popCard())
            if (cards.last() == Card(CardRank.Ace, CardSuit.Spades)) break
        }
        return if (cards.size < 10) {
            GameState.GameStart(
                currentState.table.copy(
                    buttonPosition = cards.size,
                    players = currentState.table.players.mapIndexed { i, player ->
                        if (cards.size - 1 >= i) {
                            player.copy(
                                hand = listOf(cards[i]),
                            )
                        } else {
                            player
                        }
                    },
                ),
            )
        } else {
            GameState.GameStart(
                currentState.table.copy(
                    buttonPosition = cards
                        .withIndex()
                        .maxWithOrNull(
                            compareBy(
                                compareBy<Card> { it.rank.value }
                                    .thenBy { it.suit.value },
                            ) { it.value },
                        )
                        ?.index?.plus(1) ?: 0,
                    players = currentState.table.players.mapIndexed { i, player ->
                        player.copy(
                            hand = listOf(cards[i]),
                        )
                    },
                ),
            )
        }
    }

    private fun setButton(table: Table): Table {
        val buttonPlayerIndex = table.buttonPosition - 1
        val smallBlindPlayerIndex = if (buttonPlayerIndex >= table.players.size - 1) 0 else buttonPlayerIndex + 1
        val bigBlindPlayerIndex = when {
            buttonPlayerIndex >= table.players.size - 1 -> 1
            buttonPlayerIndex + 1 >= table.players.size - 1 -> 0
            else -> buttonPlayerIndex + 2
        }
        val buttonPlayer = table.players[buttonPlayerIndex]
        val smallBlindPlayer = table.players[if (table.players.size == 2) buttonPlayerIndex else smallBlindPlayerIndex]
        val bigBlindPlayer = table.players[if (table.players.size == 2) smallBlindPlayerIndex else bigBlindPlayerIndex]
        val buttonAndBlindsPlayers = if (table.players.size == 2) {
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
        val players = if (table.players.size == 2) {
            buttonAndBlindsPlayers
        } else {
            listOf(smallBlindPlayer)
                .plus(listOf(bigBlindPlayer))
                .plus(table.players.filter { !buttonAndBlindsPlayers.contains(it) })
                .plus(listOf(buttonPlayer))
        }
        return table.copy(players = players)
    }
}
