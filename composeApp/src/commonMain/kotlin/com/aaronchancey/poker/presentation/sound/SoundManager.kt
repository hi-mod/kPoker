package com.aaronchancey.poker.presentation.sound

import com.aaronchancey.poker.presentation.room.SoundType
import kotlin.random.Random

/**
 * Maps [SoundType] values to Compose Resource paths.
 *
 * Handles resource path resolution and provides variety by randomly selecting
 * from multiple variants where available (e.g., chip sounds).
 */
object SoundManager {

    private const val CHIP_SOUND_VARIANTS = 6

    /**
     * Returns the Compose Resource path for the given [SoundType].
     *
     * For [SoundType.CHIP_MOVE], randomly selects one of the available chip sound variants
     * to provide audio variety.
     *
     * @param soundType The type of sound to play.
     * @return The resource path string (e.g., "files/chips-stack-1.mp3").
     */
    fun getPath(soundType: SoundType): String = when (soundType) {
        SoundType.CHECK -> "files/knock.mp3"

        SoundType.CHIP_MOVE -> {
            val variant = Random.nextInt(1, CHIP_SOUND_VARIANTS + 1)
            "files/chips-stack-$variant.mp3"
        }

        // TODO: Add sound files and mappings for these types
        SoundType.CARD_DEAL -> "files/chips-stack-1.mp3"

        // Placeholder
        SoundType.YOUR_TURN -> "files/chips-stack-1.mp3"

        // Placeholder
        SoundType.WIN -> "files/chips-stack-1.mp3" // Placeholder
    }
}
