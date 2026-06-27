package com.blockschedule.game

import android.content.Context

/** Settings for using Spotify as the dance-party music source. */
class SpotifyPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("spotify", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(v) = prefs.edit().putBoolean(KEY_ENABLED, v).apply()

    var clientId: String
        get() = prefs.getString(KEY_CLIENT, "") ?: ""
        set(v) = prefs.edit().putString(KEY_CLIENT, v.trim()).apply()

    /** What the user typed (a link or URI) — kept for display. */
    var playlistRaw: String
        get() = prefs.getString(KEY_RAW, "") ?: ""
        set(v) = prefs.edit().putString(KEY_RAW, v).apply()

    /** Normalized Spotify URI (spotify:playlist:...). */
    var playlistUri: String
        get() = prefs.getString(KEY_URI, "") ?: ""
        set(v) = prefs.edit().putString(KEY_URI, v).apply()

    val redirectUri: String get() = REDIRECT_URI

    val isConfigured: Boolean get() = enabled && clientId.isNotBlank() && playlistUri.isNotBlank()

    companion object {
        const val REDIRECT_URI = "blockschedule://callback"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_CLIENT = "client_id"
        private const val KEY_RAW = "playlist_raw"
        private const val KEY_URI = "playlist_uri"

        /** Turn a Spotify link or URI into a spotify:type:id URI (playlist/album/artist). */
        fun normalizeToUri(input: String): String {
            val s = input.trim()
            if (s.startsWith("spotify:")) return s
            // https://open.spotify.com/playlist/<id>?si=...
            val m = Regex("open\\.spotify\\.com/(playlist|album|artist|track)/([A-Za-z0-9]+)").find(s)
            return if (m != null) "spotify:${m.groupValues[1]}:${m.groupValues[2]}" else ""
        }
    }
}
