package com.blockschedule.game

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.blockschedule.R

/** Plays the little completion chimes (respects the sound toggle). */
object SoundPlayer {
    private var pool: SoundPool? = null
    private var completeId = 0
    private var celebrateId = 0

    fun init(context: Context) {
        if (pool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        pool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build().also {
            completeId = it.load(context, R.raw.complete, 1)
            celebrateId = it.load(context, R.raw.celebrate, 1)
        }
    }

    fun playComplete(context: Context) = play(context, completeId)
    fun playCelebrate(context: Context) = play(context, celebrateId)

    private fun play(context: Context, soundId: Int) {
        init(context)
        if (GamePrefs(context).soundEnabled && soundId != 0) {
            pool?.play(soundId, 1f, 1f, 1, 0, 1f)
        }
    }
}
