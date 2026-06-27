package com.blockschedule.game

import android.content.Context
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote

/**
 * Plays the user's Spotify playlist during the dance party via the Spotify App Remote SDK.
 * Requires Spotify Premium, the Spotify app installed, and a registered client ID (see README).
 * Reports success/failure so the caller can fall back to the built-in tune.
 */
object SpotifyController {
    private var remote: SpotifyAppRemote? = null

    fun playPlaylist(
        context: Context,
        clientId: String,
        redirectUri: String,
        playlistUri: String,
        onResult: (Boolean) -> Unit
    ) {
        val params = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(context.applicationContext, params, object : Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote) {
                remote = appRemote
                runCatching {
                    appRemote.playerApi.setShuffle(true)
                    appRemote.playerApi.play(playlistUri)
                }
                onResult(true)
            }

            override fun onFailure(error: Throwable) {
                remote = null
                onResult(false)
            }
        })
    }

    fun pause() {
        runCatching { remote?.playerApi?.pause() }
    }

    fun disconnect() {
        remote?.let { SpotifyAppRemote.disconnect(it) }
        remote = null
    }
}
