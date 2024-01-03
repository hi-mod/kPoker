package com.poker.common.data.mappers

import com.poker.common.data.remote.dto.CardDto
import com.poker.common.data.remote.dto.CardRankDto
import com.poker.common.data.remote.dto.CardSuitDto
import com.poker.common.data.remote.dto.GameDto
import com.poker.common.data.remote.dto.GameTypeDto
import com.poker.common.data.remote.dto.LevelDto
import com.poker.common.data.remote.dto.PlayerDto
import com.poker.common.data.remote.dto.PokerActionDto
import com.poker.common.domain.Card
import com.poker.common.domain.Game
import com.poker.common.domain.GameState
import com.poker.common.domain.GameType
import com.poker.common.domain.Level
import com.poker.common.domain.Player
import com.poker.common.domain.PokerAction
import java.time.Instant
import java.util.Date

fun GameState.toGameDto() = when (this) {
    GameState.Idle -> idleGameStateGameDto()
    is GameState.GameStart -> gameStartGameStateGameDto(game)
    is GameState.HandStart -> TODO()
    is GameState.Street.PreFlop -> TODO()
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
    suit = CardSuitDto.valueOf(suit.name)
)

private fun gameStartGameStateGameDto(game: Game) = GameDto(
    gameState = "GameStart",
    name = game.name,
    description = game.description,
    inProgress = game.inProgress,
    gameType = game.gameType.toGameTypeDto(),
    tableNumber = game.tableNumber,
    level = game.level.toLevelDto(),
    id = game.id,
    started = game.started,
    buttonPosition = game.buttonPosition,
    players = game.players.map { it.toPlayerDto() },
    handNumber = game.handNumber,
    board = game.board.map { it.toCardDto() },
    smallestChipSizeInPlay = game.smallestChipSizeInPlay,
    pot = game.pot,
    viewers = game.viewers,
    maxPlayers = game.maxPlayers,
    minPlayers = game.minPlayers,
)

private fun idleGameStateGameDto() = GameDto(
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
