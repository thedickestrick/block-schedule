package com.blockschedule.update

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** The latest version advertised by the remote manifest. */
data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val notes: String,
    val apkUrl: String
)

/** Fetches the update manifest and downloads the APK. Pure I/O, no UI. */
object UpdateManager {

    /** Reads version.json from the latest GitHub release. Returns null on any failure. */
    suspend fun fetchLatest(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val text = readText(UpdateConfig.VERSION_JSON_URL)
            val json = JSONObject(text)
            UpdateInfo(
                versionCode = json.getInt("versionCode"),
                versionName = json.optString("versionName", "?"),
                notes = json.optString("notes", ""),
                apkUrl = json.optString(
                    "apkUrl",
                    "https://github.com/${UpdateConfig.REPO_OWNER}/${UpdateConfig.REPO_NAME}" +
                        "/releases/latest/download/app-release.apk"
                )
            )
        }.getOrNull()
    }

    fun isNewer(info: UpdateInfo, currentVersionCode: Int): Boolean =
        info.versionCode > currentVersionCode

    /**
     * Downloads the APK into the app cache, reporting progress 0f..1f (or -1f if size unknown).
     * Returns the downloaded file.
     */
    suspend fun downloadApk(
        context: Context,
        apkUrl: String,
        onProgress: (Float) -> Unit
    ): File = withContext(Dispatchers.IO) {
        val out = File(context.cacheDir, "update.apk")
        if (out.exists()) out.delete()

        val conn = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 20_000
            readTimeout = 30_000
        }
        conn.inputStream.use { input ->
            val total = conn.contentLengthLong
            out.outputStream().use { output ->
                val buf = ByteArray(64 * 1024)
                var read: Int
                var downloaded = 0L
                while (input.read(buf).also { read = it } != -1) {
                    output.write(buf, 0, read)
                    downloaded += read
                    onProgress(if (total > 0) downloaded.toFloat() / total else -1f)
                }
            }
        }
        conn.disconnect()
        out
    }

    private fun readText(urlStr: String): String {
        val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 15_000
        }
        return conn.inputStream.bufferedReader().use { it.readText() }.also { conn.disconnect() }
    }
}
