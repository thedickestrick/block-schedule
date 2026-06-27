package com.blockschedule.game

import android.content.Context
import android.media.MediaPlayer
import com.blockschedule.R

/** Loops the dance-party tune while the party is on screen (respects the sound toggle). */
object DancePlayer {
    private var mp: MediaPlayer? = null

    fun start(context: Context) {
        if (!GamePrefs(context).soundEnabled) return
        stop()
        mp = MediaPlayer.create(context, R.raw.dance)?.apply {
            isLooping = true
            start()
        }
    }

    fun stop() {
        mp?.let { runCatching { if (it.isPlaying) it.stop() }; it.release() }
        mp = null
    }
}
