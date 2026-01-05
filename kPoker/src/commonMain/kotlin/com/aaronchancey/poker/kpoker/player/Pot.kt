package com.aaronchancey.poker.kpoker.player

data class Pot(
    val amount: ChipAmount = 0,
    val eligiblePlayerIds: Set<PlayerId> = emptySet(),
    val isMain: Boolean = true,
) {
    fun add(amount: ChipAmount): Pot = copy(amount = this.amount + amount)

    fun removePlayer(playerId: PlayerId): Pot = copy(eligiblePlayerIds = eligiblePlayerIds - playerId)
}

data class PotManager(
    val pots: List<Pot> = emptyList(),
) {
    val totalPot: ChipAmount get() = pots.sumOf { it.amount }
    val mainPot: Pot? get() = pots.firstOrNull { it.isMain }

    fun collectBets(playerBets: Map<PlayerId, ChipAmount>): PotManager {
        if (playerBets.isEmpty()) return this

        val sortedBets = playerBets.entries
            .filter { it.value > 0 }
            .sortedBy { it.value }

        if (sortedBets.isEmpty()) return this

        val newPots = mutableListOf<Pot>()
        var remainingBets = playerBets.toMutableMap()
        var previousLevel = 0L

        val betLevels = sortedBets.map { it.value }.distinct()

        for (level in betLevels) {
            val contribution = level - previousLevel
            val contributors = remainingBets.filter { it.value >= level }.keys

            if (contribution > 0 && contributors.isNotEmpty()) {
                val potAmount = contribution * contributors.size
                val isMain = newPots.isEmpty() && pots.isEmpty()

                newPots.add(
                    Pot(
                        amount = potAmount,
                        eligiblePlayerIds = contributors,
                        isMain = isMain,
                    ),
                )
            }
            previousLevel = level
        }

        // Merge with existing pots
        val allPots = if (pots.isEmpty()) {
            newPots
        } else {
            val merged = pots.toMutableList()
            for (newPot in newPots) {
                val existingMain = merged.indexOfFirst { it.isMain && it.eligiblePlayerIds == newPot.eligiblePlayerIds }
                if (existingMain >= 0) {
                    merged[existingMain] = merged[existingMain].add(newPot.amount)
                } else {
                    merged.add(newPot.copy(isMain = false))
                }
            }
            merged
        }

        return copy(pots = allPots)
    }

    fun removePlayer(playerId: PlayerId): PotManager = copy(pots = pots.map { it.removePlayer(playerId) })

    fun reset(): PotManager = PotManager()
}
