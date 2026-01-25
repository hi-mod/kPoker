package com.aaronchancey.poker.presentation.sound

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.Foundation.NSData
import platform.Foundation.create
import platform.darwin.NSObject
import poker.composeapp.generated.resources.Res

/**
 * iOS implementation of [SoundPlayer] using AVFoundation's [AVAudioPlayer].
 *
 * Maintains references to active players to prevent deallocation during playback.
 * Players are automatically removed after playback completes.
 */
@OptIn(ExperimentalForeignApi::class)
class SoundPlayerImpl : SoundPlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val activePlayers = mutableListOf<AVAudioPlayer>()

    override fun playSound(path: String) {
        scope.launch {
            try {
                val bytes = Res.readBytes(path)
                val nsData = bytes.toNSData()
                val player = AVAudioPlayer(data = nsData, error = null)
                player.delegate = createDelegate(player)
                activePlayers.add(player)
                player.play()
            } catch (e: Exception) {
                println("SoundPlayerImpl: Failed to play sound: ${e.message}")
            }
        }
    }

    private fun createDelegate(player: AVAudioPlayer): AVAudioPlayerDelegateProtocol = object : NSObject(), AVAudioPlayerDelegateProtocol {
        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
            activePlayers.remove(player)
        }
    }

    @OptIn(BetaInteropApi::class)
    private fun ByteArray.toNSData(): NSData = usePinned { pinned ->
        NSData.create(
            bytes = pinned.addressOf(0),
            length = size.toULong(),
        )
    }
}
