package com.aaronchancey.poker.kpoker.game

import kotlinx.serialization.Serializable

@Serializable
enum class GameVariant(val displayName: String) {
    TEXAS_HOLDEM("Texas Hold'em"),
    OMAHA("Omaha"),
    OMAHA_HI_LO("Omaha Hi-Lo"),
    ;

    companion object {
        fun fromName(name: String): GameVariant = entries.find { it.displayName == name } ?: TEXAS_HOLDEM
    }
}
