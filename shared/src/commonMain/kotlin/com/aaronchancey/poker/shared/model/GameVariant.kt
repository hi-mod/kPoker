package com.aaronchancey.poker.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class GameVariant(val displayName: String) {
    TEXAS_HOLDEM_NL("Texas Hold'em No Limit"),
    OMAHA_PL("Omaha Pot Limit"),
    OMAHA_HILO_PL("Omaha Hi-Lo Pot Limit"),
}
