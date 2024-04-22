package com.poker.common.statemachine

import com.poker.common.domain.Card
import com.poker.common.domain.CardRank
import com.poker.common.domain.CardSuit
import com.poker.common.domain.Deck
import com.poker.common.domain.GameEvent
import com.poker.common.domain.GameState
import com.poker.common.domain.GameType
import com.poker.common.domain.IGameState
import com.poker.common.domain.Level
import com.poker.common.domain.Player
import com.poker.common.domain.PokerAction
import com.poker.common.domain.Table
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.testCoroutineScheduler
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldNotContainInOrder
import io.kotest.matchers.doubles.shouldBeExactly
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.beOfType
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.exhaustive
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import java.util.UUID
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi

@OptIn(ExperimentalStdlibApi::class, ExperimentalCoroutinesApi::class, ExperimentalSerializationApi::class)
internal class GameStateMachineTests : FunSpec({

    coroutineTestScope = true

    context("com.poker.common.statemachine.GameStateMachineTests") {
        val startingChips = 1000.0
        val players = List(5) { index ->
            val id = index + 1
            Player(id.toString(), "Player $id", 1000.0)
        }

        val inputChannel = Channel<GameEvent>()
        val gameStates = mutableListOf<GameState>()

        var table: Table = mockk()

        fun newPlayer() = Player(
            id = UUID.randomUUID().toString(),
            name = UUID.randomUUID().toString(),
            chips = 1000.0,
        )

        fun assertForHandStarting(
            sut: GameStateMachine,
            table: Table,
        ) {
            val gameState = (sut.gameState as IGameState).table
            withClue("game.level should not be null") {
                gameState.level shouldNotBe null
            }
            assertSoftly {
                gameState.deck.cards shouldNotContainInOrder Deck().cards
                gameState.pot shouldBe 30.0
                table.level.let { level ->
                    withClue("dealer will post the small blind and ante") {
                        gameState.players[0].chips shouldBeExactly startingChips - level.smallBlind - (level.ante ?: 0.0)
                        gameState.players[0].currentWager shouldBeExactly level.smallBlind + (level.ante ?: 0.0)
                    }
                    withClue("player to the left of the button will post the big blind and ante") {
                        gameState.players[1].chips shouldBeExactly startingChips - level.bigBlind - (level.ante ?: 0.0)
                        gameState.players[1].currentWager shouldBeExactly level.bigBlind + (level.ante ?: 0.0)
                    }
                }
            }
        }

        fun sendEventAndCollectStates(sut: GameStateMachine, event: GameEvent) {
            launch {
                inputChannel.send(event)
            }
            launch {
                sut.stateMachine(inputChannel)
                    .take(2)
                    .collect { gameState ->
                        gameStates.add(gameState)
                    }
            }
            testCoroutineScheduler.runCurrent()
        }

        beforeEach {
            gameStates.clear()
            table = Table(
                name = "",
                description = "",
                inProgress = true,
                gameType = GameType.Tournament,
                tableNumber = 1u,
                level = Level(10.0, 20.0, 0.0, 10),
                buttonPosition = 4,
                players = players,
                minPlayers = 5,
            )
        }

        test("GameState transitions from Idle to GameStarting when StartGame event is sent") {
            val sut = GameStateMachine(GameState.Idle)
            sendEventAndCollectStates(sut, GameEvent.StartGame(table))
            gameStates shouldContainExactly listOf(
                GameState.Idle,
                GameState.GameStart(table),
            )
        }

        test("given GameState is GameStarting when GameEvent.AddPlayer is sent then GameState is GameState.GameStarting and a new Player is added to the GameState") {
            val player = newPlayer()
            val sut = GameStateMachine(GameState.GameStart(table))
            sendEventAndCollectStates(sut, GameEvent.AddPlayer(player))
            gameStates shouldContainExactly listOf(
                GameState.GameStart(table),
                GameState.GameStart(table.copy(players = players.plus(player))),
            )
        }

        test("given GameState is GameStarting when GameEvent.AddViewer is sent then GameState is GameState.GameStarting and a new Viewer is added to the GameState") {
            val sut = GameStateMachine(GameState.GameStart(table))
            val viewer = UUID.randomUUID().toString()
            sendEventAndCollectStates(sut, GameEvent.AddViewer(viewer))
            gameStates shouldContainExactly listOf(
                GameState.GameStart(table),
                GameState.GameStart(table.copy(viewers = listOf(viewer))),
            )
        }

        test("given GameState is GameStarting when GameEvent.RemovePlayer is sent then GameState is GameState.GameStarting and a Player is removed from the GameState") {
            val sut = GameStateMachine(GameState.GameStart(table))
            sendEventAndCollectStates(sut, GameEvent.RemovePlayer(table.players[1]))
            gameStates shouldContainExactly listOf(
                GameState.GameStart(table),
                GameState.GameStart(table.copy(players = players.minus(players[1]))),
            )
        }

        test("given GameState is GameStarting when GameEvent.ChooseStartingDealer is sent then GameState is GameState.GameStarting and the starting dealer is set in GameState") {
            val deckMock = mockk<Deck>(relaxed = true)
            val cards = CardSuit
                .entries
                .flatMap { s -> CardRank.entries.map { r -> Card(r, s) } }
                .toMutableList()
            every { deckMock.cards } returns cards
            every { deckMock.shuffle() } just runs
            every { deckMock.popCard() } returnsMany cards
            val expectedGame = table.copy(
                deck = deckMock,
            )
            val sut = GameStateMachine(GameState.GameStart(expectedGame))
            sendEventAndCollectStates(sut, GameEvent.ChooseStartingDealer)
            gameStates shouldContainExactly listOf(
                GameState.GameStart(expectedGame),
                GameState.GameStart(
                    expectedGame.copy(
                        buttonPosition = 5,
                        players = expectedGame.players.mapIndexed { i, player ->
                            player.copy(
                                hand = listOf(cards[i]),
                            )
                        },
                    ),
                ),
            )
        }

        test("given GameState is GameStarting when GameEvent.SetButton is sent then GameState is GameState.GameStarting and the players list is reordered to reflect starting dealer is set in GameState") {
            // Given
            val sut = GameStateMachine(GameState.GameStart(table))

            // When
            sendEventAndCollectStates(sut, GameEvent.SetButton)

            // Then
            withClue("then game.players should be in this order exactly") {
                val expectGameData = table.copy(
                    players = listOf(
                        players[4],
                        players[0],
                        players[1],
                        players[2],
                        players[3],
                    ),
                )
                sut.gameState shouldBe GameState.GameStart(expectGameData)
            }
        }

        test("given GameState is GameStarting when GameEvent.StartGameComplete is sent then GameState is GameState.PreFlop") {
            // Given
            val sut = GameStateMachine(GameState.GameStart(table))

            // When
            sendEventAndCollectStates(sut, GameEvent.GameReady)

            // Then
            sut.gameState should beOfType<GameState.Street.PreFlop>()

            assertForHandStarting(sut, table)
        }

        test("given GameState transition list when GameEvent.AddViewer is sent then GameState is GameState.PreFlop and a new Viewer is added to the GameState") {
            val viewerId = UUID.randomUUID().toString()
            val gameStateList = listOf(
                GameState.Street.PreFlop(table) to GameState.Street.PreFlop(table.copy(viewers = table.viewers.plus(viewerId))),
                GameState.Street.Flop(table) to GameState.Street.Flop(table.copy(viewers = table.viewers.plus(viewerId))),
                GameState.Street.Turn(table) to GameState.Street.Turn(table.copy(viewers = table.viewers.plus(viewerId))),
                GameState.Street.River(table) to GameState.Street.River(table.copy(viewers = table.viewers.plus(viewerId))),
            ).exhaustive()
            checkAll(gameStateList) { state ->
                // Given
                val sut = GameStateMachine(state.first)

                // When
                sendEventAndCollectStates(sut, GameEvent.AddViewer(viewerId))

                // Then
                sut.gameState shouldBe state.second
            }
        }

        test("given GameState transition list when GameEvent.SelectPlayerAction check is sent then GameState is GameState.PreFlop, pot does NOT increase and next player becomes active") {
            val expectedPlayers = table.players.subList(1, 5) +
                table.players.first().copy(
                    hasActed = true,
                )
            val gameStateList = listOf(
                GameState.Street.PreFlop(table) to GameState.Street.PreFlop(table.copy(players = expectedPlayers)),
                GameState.Street.Flop(table) to GameState.Street.Flop(table.copy(players = expectedPlayers)),
                GameState.Street.Turn(table) to GameState.Street.Turn(table.copy(players = expectedPlayers)),
                GameState.Street.River(table) to GameState.Street.River(table.copy(players = expectedPlayers)),
            ).exhaustive()
            checkAll(gameStateList) { state ->
                // Given
                val sut = GameStateMachine(state.first)

                // When
                sendEventAndCollectStates(sut, GameEvent.SelectPlayerAction.Check)

                // Then
                sut.gameState shouldBe state.second
            }
        }

        test("given GameState transition list when GameEvent.SelectPlayerAction bet is sent then GameState is GameState.PreFlop, pot DOES increase and next player becomes active") {
            val expectedGameData = table.copy(
                players = table.players.subList(1, 5) + table.players.first().copy(
                    chips = 990.0,
                    currentWager = 10.0,
                    hasActed = true,
                ),
                pot = 10.0,
            )
            val gameStateList = listOf(
                GameState.Street.PreFlop(table) to GameState.Street.PreFlop(expectedGameData),
                GameState.Street.Flop(table) to GameState.Street.Flop(expectedGameData),
                GameState.Street.Turn(table) to GameState.Street.Turn(expectedGameData),
                GameState.Street.River(table) to GameState.Street.River(expectedGameData),
            ).exhaustive()
            checkAll(gameStateList) { state ->
                // Given
                val sut = GameStateMachine(state.first)

                // When
                sendEventAndCollectStates(sut, GameEvent.SelectPlayerAction.Bet(10.0))

                // Then
                sut.gameState shouldBe state.second
            }
        }

        test("given GameState transition list when GameEvent.SelectPlayerAction call is sent then GameState is GameState.PreFlop, pot DOES increase and next player becomes active") {
            val givenPlayers = players.subList(1, players.size) +
                players.first().copy(
                    chips = 990.0,
                    currentWager = 10.0,
                    hasActed = true,
                )
            val startingGame = table.copy(
                players = givenPlayers,
                pot = table.pot + 10.0,
            )
            val expectedGameData = table.copy(
                players = givenPlayers.subList(1, givenPlayers.size) + givenPlayers
                    .first().copy(
                        chips = 990.0,
                        currentWager = 10.0,
                        hasActed = true,
                    ),
                pot = table.pot + 20.0,
            )
            val gameStateList = listOf(
                GameState.Street.PreFlop(startingGame) to GameState.Street.PreFlop(expectedGameData),
                GameState.Street.Flop(startingGame) to GameState.Street.Flop(expectedGameData),
                GameState.Street.Turn(startingGame) to GameState.Street.Turn(expectedGameData),
                GameState.Street.River(startingGame) to GameState.Street.River(expectedGameData),
            ).exhaustive()
            checkAll(gameStateList) { state ->
                // Given
                val sut = GameStateMachine(state.first)

                // When
                sendEventAndCollectStates(sut, GameEvent.SelectPlayerAction.Call)

                // Then
                sut.gameState shouldBe state.second
            }
        }

        test("given GameState transition list when GameEvent.SelectPlayerAction fold is sent then GameState is GameState.PreFlop, pot does NOT increase and the folding player's hasFolded is set to true") {
            val expectedGameData = table.copy(
                players = table.players.subList(1, table.players.size) +
                    table.players.first().copy(
                        hasActed = true,
                        hasFolded = true,
                    ),
            )
            // Given
            val gameStateList = listOf(
                GameState.Street.PreFlop(table) to GameState.Street.PreFlop(expectedGameData),
                GameState.Street.Flop(table) to GameState.Street.Flop(expectedGameData),
                GameState.Street.Turn(table) to GameState.Street.Turn(expectedGameData),
                GameState.Street.River(table) to GameState.Street.River(expectedGameData),
            ).exhaustive()
            checkAll(gameStateList) { states ->
                val sut = GameStateMachine(states.first)

                // When
                sendEventAndCollectStates(sut, GameEvent.SelectPlayerAction.Fold)

                // Then
                sut.gameState shouldBe states.second
            }
        }

        test("given GameState transition list when GameEvent.SelectPlayerAction raise is sent then GameState is GameState.PreFlop, pot DOES increase, player bets more than the previous player and next player becomes active") {
            // Given
            val givenPlayers = players.subList(1, players.size) + players.first().copy(
                chips = 990.0,
                currentWager = 10.0,
                hasActed = true,
            )
            val gameStart = table.copy(
                players = givenPlayers,
                pot = table.pot + 10.0,
            )
            val expectedGameData = table.copy(
                players = givenPlayers.subList(1, givenPlayers.size) + givenPlayers
                    .first().copy(
                        chips = 980.0,
                        currentWager = 20.0,
                        hasActed = true,
                    ),
                pot = table.pot + 30.0,
            )
            val gameStateList = listOf(
                GameState.Street.PreFlop(gameStart) to GameState.Street.PreFlop(expectedGameData),
                GameState.Street.Flop(gameStart) to GameState.Street.Flop(expectedGameData),
                GameState.Street.Turn(gameStart) to GameState.Street.Turn(expectedGameData),
                GameState.Street.River(gameStart) to GameState.Street.River(expectedGameData),
            ).exhaustive()
            checkAll(gameStateList) { states ->
                val sut = GameStateMachine(
                    states.first,
                )

                // When
                sendEventAndCollectStates(sut, GameEvent.SelectPlayerAction.Raise(20.0))

                // Then
                sut.gameState shouldBe states.second
            }
        }

        test("given GameState transition list when all players have acted GameState transitions to Flop and 3 cards are dealt") {
            // Given
            val givenPlayers = listOf(
                players.first(),
            ) + players.subList(1, players.size).map { player ->
                player.copy(
                    chips = 990.0,
                    currentWager = 10.0,
                    hasActed = true,
                )
            }
            val gameStart = table.copy(
                players = givenPlayers,
                pot = 50.0,
            )
            val expectedGameData = table.copy(
                players = givenPlayers.subList(1, givenPlayers.size) + givenPlayers
                    .first().copy(
                        chips = 990.0,
                        currentWager = 10.0,
                        hasActed = true,
                    ),
                pot = 60.0,
            )
            val gameStateList = listOf(
                GameState.Street.PreFlop(gameStart) to GameState.Street.Flop(expectedGameData),
                GameState.Street.Flop(gameStart) to GameState.Street.Turn(expectedGameData),
                GameState.Street.Turn(gameStart) to GameState.Street.River(expectedGameData),
            ).exhaustive()
            checkAll(gameStateList) { states ->
                val sut = GameStateMachine(states.first)

                // When
                sendEventAndCollectStates(sut, GameEvent.SelectPlayerAction.Call)

                // Then
                sut.gameState shouldBe states.second
            }
        }

        test("GameState transitions from River to ShowDown when all players have acted") {
            // Given
            val givenPlayers = listOf(
                players.first(),
            ) + players.subList(1, players.size).map { player ->
                player.copy(
                    chips = 990.0,
                    currentWager = 10.0,
                    hasActed = true,
                )
            }
            val gameStart = table.copy(
                players = givenPlayers,
                pot = 50.0,
            )
            val expectedGameData = table.copy(
                players = givenPlayers.subList(1, givenPlayers.size) + givenPlayers
                    .first().copy(
                        chips = 990.0,
                        currentWager = 10.0,
                        hasActed = true,
                    ),
                pot = 60.0,
            )
            val sut = GameStateMachine(GameState.Street.River(gameStart))

            // When
            sendEventAndCollectStates(sut, GameEvent.SelectPlayerAction.Call)
            // Then
            sut.gameState shouldBe GameState.Showdown(expectedGameData)
        }

        test("when game state is ShowDown and AwardPot event is sent, it should determine the winner and award the pot") {
            // Given
            val board = listOf(
                Card.JackOfDiamonds,
                Card.KingOfClubs,
                Card.KingOfSpades,
                Card.KingOfHearts,
                Card.QueenOfHearts,
            )
            val gameData = table.copy(
                board = board,
                pot = 5.0,
                players = listOf(
                    Player("5", "", 1000.0, listOf(Card.JackOfClubs, Card.JackOfDiamonds)),
                    Player("1", "", 1000.0, listOf(Card.AceOfClubs, Card.AceOfDiamonds)),
                    Player("2", "", 1000.0, listOf(Card.QueenOfDiamonds, Card.KingOfDiamonds)),
                    Player("3", "", 1000.0, listOf(Card.AceOfSpades, Card.EightOfDiamonds)),
                    Player("4", "", 1000.0, listOf(Card.AceOfHearts, Card.NineOfDiamonds)),
                ),
            )
            val sut = GameStateMachine(GameState.Showdown(gameData))

            // When
            sendEventAndCollectStates(sut, GameEvent.AwardPot)

            // Then
            val expectedGameData = gameData.copy(
                players = listOf(
                    Player("5", "", 1000.0, listOf(Card.JackOfClubs, Card.JackOfDiamonds)),
                    Player("1", "", 1000.0, listOf(Card.AceOfClubs, Card.AceOfDiamonds)),
                    Player("2", "", 1005.0, listOf(Card.QueenOfDiamonds, Card.KingOfDiamonds)),
                    Player("3", "", 1000.0, listOf(Card.AceOfSpades, Card.EightOfDiamonds)),
                    Player("4", "", 1000.0, listOf(Card.AceOfHearts, Card.NineOfDiamonds)),
                ),
            )
            sut.gameState shouldBe GameState.Showdown(expectedGameData)
        }

        test("when game state is ShowDown and EndHand event is sent, it should transition to HandComplete, clear the board and remove player cards") {
            // Given
            val board = listOf(
                Card.JackOfDiamonds,
                Card.KingOfClubs,
                Card.KingOfSpades,
                Card.KingOfHearts,
                Card.QueenOfHearts,
            )
            val gameData = table.copy(
                board = board,
                pot = 0.0,
                players = listOf(
                    Player("5", "", 1000.0, listOf(Card.JackOfClubs, Card.JackOfDiamonds)),
                    Player("1", "", 1000.0, listOf(Card.AceOfClubs, Card.AceOfDiamonds)),
                    Player("2", "", 1005.0, listOf(Card.QueenOfDiamonds, Card.KingOfDiamonds)),
                    Player("3", "", 1000.0, listOf(Card.AceOfSpades, Card.EightOfDiamonds)),
                    Player("4", "", 1000.0, listOf(Card.AceOfHearts, Card.NineOfDiamonds)),
                ),
            )
            val sut = GameStateMachine(GameState.Showdown(gameData))

            // When
            sendEventAndCollectStates(sut, GameEvent.EndHand)

            // Then
            val expectedGameData = gameData.copy(
                board = emptyList(),
                players = listOf(
                    Player("5", "", 1000.0),
                    Player("1", "", 1000.0),
                    Player("2", "", 1005.0),
                    Player("3", "", 1000.0),
                    Player("4", "", 1000.0),
                ),
            )
            sut.gameState shouldBe GameState.HandComplete(expectedGameData)
        }

        test("when game state is HandComplete and StartHand event is sent, it should start the next hand") {
            // Given
            val deckMock = mockk<Deck>()
            val cards = CardSuit
                .entries
                .flatMap { s -> CardRank.entries.map { r -> Card(r, s) } }
                .toMutableList()
            every { deckMock.cards } returns cards
            every { deckMock.shuffle() } just runs
            every { deckMock.popCard() } returnsMany cards
            val gameData = table.copy(
                deck = deckMock,
                pot = 0.0,
            )
            val sut = GameStateMachine(GameState.HandComplete(gameData))

            // When
            sendEventAndCollectStates(sut, GameEvent.StartHand)

            // Then
            val expectedGameData = table.copy(
                buttonPosition = 4,
                deck = deckMock,
                handNumber = 1,
                players = listOf(
                    Player(
                        id = "1",
                        name = "Player 1",
                        chips = 990.0,
                        hand = listOf(Card.TwoOfClubs, Card.SevenOfClubs),
                        currentWager = 10.0,
                        availablePlayerActions = listOf(PokerAction.Call, PokerAction.Fold, PokerAction.Raise),
                    ),
                    Player(
                        id = "2",
                        name = "Player 2",
                        chips = 980.0,
                        hand = listOf(Card.ThreeOfClubs, Card.EightOfClubs),
                        currentWager = 20.0,
                    ),
                    Player(
                        id = "3",
                        name = "Player 3",
                        chips = 1000.0,
                        hand = listOf(Card.FourOfClubs, Card.NineOfClubs),
                    ),
                    Player(
                        id = "4",
                        name = "Player 4",
                        chips = 1000.0,
                        hand = listOf(Card.FiveOfClubs, Card.TenOfClubs),
                    ),
                    Player(
                        id = "5",
                        name = "Player 5",
                        chips = 1000.0,
                        hand = listOf(Card.SixOfClubs, Card.JackOfClubs),
                    ),
                ),
                pot = 30.0,
            )
            sut.gameState shouldBe GameState.HandStart(expectedGameData)
        }
    }

    afterTest {
        clearAllMocks()
    }
})
