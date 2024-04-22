package com.poker.common.domain

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class GameTests : FunSpec({
    context("GameTests") {
        test("give a Game and dealCards is called cards are dealt") {
            val players = List(5) { index ->
                val id = index + 1
                Player(id.toString(), "Player $id", 1000.0)
            }
            val table = Table(
                name = "",
                description = "",
                inProgress = true,
                gameType = GameType.Tournament,
                tableNumber = 1u,
                level = Level(10.0, 20.0, 0.0, 10),
                buttonPosition = 4,
                players = players,
                minPlayers = 7,
            ).dealCards()

            table.deck.cards.size shouldBe 42
            table.players.forEach { player -> player.hand.size shouldBe 2 }
        }
    }
})
