package com.aaronchancey.poker.presentation.sound

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import poker.composeapp.generated.resources.Res

/**
 * Android implementation of [SoundPlayer] using [MediaPlayer].
 *
 * Creates a new [MediaPlayer] instance per sound to allow overlapping playback.
 * Each player is released automatically after playback completes.
 */
class SoundPlayerImpl(private val context: Context) : SoundPlayer {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun playSound(path: String) {
        scope.launch {
            try {
                val uri = Uri.parse(Res.getUri(path))
                MediaPlayer().apply {
                    setDataSource(context, uri)
                    setOnCompletionListener { mp -> mp.release() }
                    setOnErrorListener { mp, _, _ ->
                        mp.release()
                        true
                    }
                    prepare()
                    start()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
