package com.aaronchancey.poker.kpoker.formats

import com.aaronchancey.poker.kpoker.player.ChipAmount

data class BlindLevel(
    val level: Int,
    val smallBlind: ChipAmount,
    val bigBlind: ChipAmount,
    val ante: ChipAmount = 0,
    val durationMinutes: Int,
)

data class PayoutStructure(
    val place: Int,
    val percentOfPrizePool: Double,
)

data class TournamentConfig(
    override val minPlayers: Int,
    override val maxPlayers: Int,
    val buyIn: ChipAmount,
    val startingChips: ChipAmount,
    val blindLevels: List<BlindLevel>,
    val payoutStructure: List<PayoutStructure>,
    val lateRegistrationLevels: Int = 0, // Number of levels allowing late reg
    val rebuyAllowed: Boolean = false,
    val rebuyLevels: Int = 0,
    val rebuyCost: ChipAmount = 0,
    val rebuyChips: ChipAmount = 0,
    val addonAllowed: Boolean = false,
    val addonCost: ChipAmount = 0,
    val addonChips: ChipAmount = 0,
    val tablesCount: Int = 1,
) : GameFormatConfig {
    override val format = if (tablesCount == 1 && maxPlayers <= 10) {
        GameFormat.SIT_AND_GO
    } else {
        GameFormat.MULTI_TABLE_TOURNAMENT
    }

    val prizePool: ChipAmount get() = buyIn * maxPlayers

    companion object {
        fun sitAndGo(
            players: Int,
            buyIn: ChipAmount,
            startingChips: ChipAmount = 1500,
        ): TournamentConfig = TournamentConfig(
            minPlayers = players,
            maxPlayers = players,
            buyIn = buyIn,
            startingChips = startingChips,
            blindLevels = defaultBlindLevels(startingChips),
            payoutStructure = defaultPayouts(players),
        )

        private fun defaultBlindLevels(startingChips: ChipAmount): List<BlindLevel> {
            val baseSmall = startingChips / 100 // 1% of starting stack
            return listOf(
                BlindLevel(1, baseSmall, baseSmall * 2, 0, 10),
                BlindLevel(2, baseSmall * 2, baseSmall * 4, 0, 10),
                BlindLevel(3, baseSmall * 3, baseSmall * 6, 0, 10),
                BlindLevel(4, baseSmall * 4, baseSmall * 8, baseSmall, 10),
                BlindLevel(5, baseSmall * 5, baseSmall * 10, baseSmall, 10),
                BlindLevel(6, baseSmall * 8, baseSmall * 16, baseSmall * 2, 10),
                BlindLevel(7, baseSmall * 10, baseSmall * 20, baseSmall * 2, 10),
                BlindLevel(8, baseSmall * 15, baseSmall * 30, baseSmall * 3, 10),
                BlindLevel(9, baseSmall * 20, baseSmall * 40, baseSmall * 4, 10),
                BlindLevel(10, baseSmall * 30, baseSmall * 60, baseSmall * 6, 10),
            )
        }

        private fun defaultPayouts(players: Int): List<PayoutStructure> = when {
            players <= 2 -> listOf(PayoutStructure(1, 100.0))

            players <= 4 -> listOf(
                PayoutStructure(1, 65.0),
                PayoutStructure(2, 35.0),
            )

            players <= 6 -> listOf(
                PayoutStructure(1, 50.0),
                PayoutStructure(2, 30.0),
                PayoutStructure(3, 20.0),
            )

            else -> listOf(
                PayoutStructure(1, 50.0),
                PayoutStructure(2, 30.0),
                PayoutStructure(3, 20.0),
            )
        }
    }
}
