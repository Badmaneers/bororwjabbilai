package com.heliactyl.bororwjabbilai

import android.content.Context
import com.heliactyl.bororwjabbilai.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("body") val body: String,
    @SerializedName("html_url") val htmlUrl: String
)

class UpdateManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
    private val currentVersion = BuildConfig.VERSION_NAME
    private val repoUrl = "https://api.github.com/repos/Badmaneers/bororwjabbilai/releases/latest"

    suspend fun checkForUpdate(): GitHubRelease? {
        val lastCheck = prefs.getLong("last_check", 0)
        val now = System.currentTimeMillis()

        // Check only once every 24 hours
        if (now - lastCheck < TimeUnit.DAYS.toMillis(1)) {
            return null
        }

        return withContext(Dispatchers.IO) {
            try {
                val url = URL(repoUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "BoroRwjabBilai-App")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                if (connection.responseCode == 200) {
                    val json = connection.inputStream.bufferedReader().use { it.readText() }
                    val release = Gson().fromJson(json, GitHubRelease::class.java)
                    
                    prefs.edit().putLong("last_check", now).apply()

                    if (isNewerVersion(release.tagName)) {
                        release
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    private fun isNewerVersion(remoteTag: String): Boolean {
        // Simple version comparison: remoteTag might be "v1.4.0" or "1.4.0"
        val remote = remoteTag.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val current = currentVersion.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until minOf(remote.size, current.size)) {
            if (remote[i] > current[i]) return true
            if (remote[i] < current[i]) return false
        }
        return remote.size > current.size
    }
}
