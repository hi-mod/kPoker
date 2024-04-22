package com.poker.common.data.mappers

import com.poker.common.data.remote.dto.game.GameDto
import com.poker.common.data.remote.dto.poker.CardDto
import com.poker.common.data.remote.dto.poker.CardRankDto
import com.poker.common.data.remote.dto.poker.CardSuitDto
import com.poker.common.data.remote.dto.poker.TableDto
import com.poker.common.data.remote.dto.poker.GameTypeDto
import com.poker.common.data.remote.dto.poker.LevelDto
import com.poker.common.data.remote.dto.poker.PlayerDto
import com.poker.common.data.remote.dto.poker.PokerActionDto
import com.poker.common.domain.Card
import com.poker.common.domain.Game
import com.poker.common.domain.Table
import com.poker.common.domain.GameState
import com.poker.common.domain.GameType
import com.poker.common.domain.Level
import com.poker.common.domain.Player
import com.poker.common.domain.PokerAction
import java.time.Instant
import java.util.Date

fun GameDto.toGame() = Game(
    id = id,
    name = name,
    description = description,
    numPlayers = numPlayers,
    level = level.toLevel(),
)

fun LevelDto.toLevel() = Level(
    smallBlind = smallBlind,
    bigBlind = bigBlind,
    ante = ante,
    duration = duration,
)

fun GameState.toGameDto() = when (this) {
    GameState.Idle -> idleGameStateGameDto()
    is GameState.GameStart -> gameStateToGameDto(this.javaClass.name, table)
    is GameState.HandStart -> gameStateToGameDto(this.javaClass.name, table)
    is GameState.Street.PreFlop -> gameStateToGameDto(this.javaClass.name, table)
    is GameState.Street.Flop -> TODO()
    is GameState.Street.Turn -> TODO()
    is GameState.Street.River -> TODO()
    is GameState.Showdown -> TODO()
    is GameState.HandComplete -> TODO()
    GameState.EndState -> TODO()
}

fun GameType.toGameTypeDto() = when (this) {
    GameType.RingGame -> GameTypeDto.RingGame
    GameType.Tournament -> GameTypeDto.Tournament
    GameType.SitNGo -> GameTypeDto.SitNGo
}

fun Level.toLevelDto() = LevelDto(
    smallBlind = smallBlind,
    bigBlind = bigBlind,
    ante = ante,
    duration = duration,
)

fun Player.toPlayerDto() = PlayerDto(
    name = name,
    id = id,
    chips = chips,
    hand = hand.map { it.toCardDto() },
    availablePlayerActions = availablePlayerActions.map { it.toPokerActionDto() },
    currentWager = currentWager,
    hasActed = hasActed,
    hasFolded = hasFolded,
)

fun PokerAction.toPokerActionDto() = PokerActionDto.valueOf(name)

fun Card.toCardDto() = CardDto(
    rank = CardRankDto.valueOf(rank.name),
    suit = CardSuitDto.valueOf(suit.name),
)

private fun gameStateToGameDto(gameState: String, table: Table) = TableDto(
    gameState = gameState,
    name = table.name,
    description = table.description,
    inProgress = table.inProgress,
    gameType = table.gameType.toGameTypeDto(),
    tableNumber = table.tableNumber,
    level = table.level.toLevelDto(),
    id = table.id,
    started = table.started,
    buttonPosition = table.buttonPosition,
    players = table.players.map { it.toPlayerDto() },
    handNumber = table.handNumber,
    board = table.board.map { it.toCardDto() },
    smallestChipSizeInPlay = table.smallestChipSizeInPlay,
    pot = table.pot,
    viewers = table.viewers,
    maxPlayers = table.maxPlayers,
    minPlayers = table.minPlayers,
)

private fun idleGameStateGameDto() = TableDto(
    gameState = "Idle",
    name = "",
    description = "",
    inProgress = false,
    gameType = GameTypeDto.RingGame,
    tableNumber = 1u,
    level = LevelDto(),
    id = "",
    started = Date.from(Instant.now()),
    buttonPosition = 1,
    players = emptyList(),
    handNumber = 0,
    board = emptyList(),
    smallestChipSizeInPlay = 0.01,
    pot = 0.0,
    viewers = emptyList(),
    maxPlayers = 10,
    minPlayers = 2,
)
