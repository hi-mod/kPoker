package com.aaronchancey.poker.presentation.sound

import poker.composeapp.generated.resources.Res

/**
 * WASM/Browser implementation of [SoundPlayer] using the HTML5 Audio API.
 *
 * Creates a new [Audio] element for each sound, which handles loading and playback.
 * Audio elements are garbage collected after playback completes.
 */
class SoundPlayerImpl : SoundPlayer {

    override fun playSound(path: String) {
        val uri = Res.getUri(path)
        playAudioJs(uri)
    }
}

/**
 * Plays audio using the HTML5 Audio API.
 *
 * Creates a new Audio element, sets its source, and plays it.
 * Errors are caught and logged to avoid crashing the app.
 */
@OptIn(ExperimentalWasmJsInterop::class)
@JsFun(
    """
(url) => {
    try {
        const audio = new Audio(url);
        audio.play().catch(e => console.log('Audio play failed:', e));
    } catch (e) {
        console.log('Audio creation failed:', e);
    }
}
""",
)
private external fun playAudioJs(url: String)
