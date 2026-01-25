package com.aaronchancey.poker.presentation.sound

import java.io.ByteArrayInputStream
import javazoom.jl.decoder.JavaLayerException
import javazoom.jl.player.Player
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import poker.composeapp.generated.resources.Res

/**
 * JVM implementation of [SoundPlayer] using JLayer for MP3 playback.
 *
 * Uses a dedicated coroutine scope with [Dispatchers.IO] since JLayer's
 * [Player.play] is a blocking call that holds the thread until playback completes.
 */
class SoundPlayerImpl : SoundPlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun playSound(path: String) {
        scope.launch {
            ByteArrayInputStream(Res.readBytes(path)).use { stream ->
                try {
                    Player(stream).play()
                } catch (e: JavaLayerException) {
                    e.printStackTrace()
                }
            }
        }
    }
}
