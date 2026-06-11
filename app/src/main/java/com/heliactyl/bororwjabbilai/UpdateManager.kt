package com.heliactyl.bororwjabbilai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

data class GitHubAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String
)

data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("body") val body: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("assets") val assets: List<GitHubAsset>
)

class UpdateManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("updates", Context.MODE_PRIVATE)
    private val currentVersion = BuildConfig.VERSION_NAME
    private val repoUrl = "https://api.github.com/repos/Badmaneers/bororwjabbilai/releases/latest"
    private val cacheDir = context.cacheDir
    private val apkName = "update.apk"

    suspend fun checkForUpdate(force: Boolean = false): GitHubRelease? {
        val lastCheck = prefs.getLong("last_check", 0)
        val now = System.currentTimeMillis()

        if (!force && (now - lastCheck < TimeUnit.DAYS.toMillis(1))) {
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

    fun downloadApk(downloadUrl: String): Flow<Float> = flow {
        // Must NOT use withContext inside a flow {} builder — it violates flow's
        // coroutine context and throws IllegalStateException at runtime.
        // flowOn(Dispatchers.IO) at the end moves the whole upstream to IO correctly.

        // GitHub's browser_download_url issues a 301/302 redirect.
        // HttpURLConnection won't auto-follow across http->https boundaries,
        // so we resolve the redirect chain manually before streaming.
        var resolvedUrl = downloadUrl
        repeat(5) { // up to 5 redirect hops
            val conn = (URL(resolvedUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "GET"
                setRequestProperty("User-Agent", "BoroRwjabBilai-App")
                connectTimeout = 10_000
                readTimeout = 10_000
                connect()
            }
            val code = conn.responseCode
            if (code in 300..399) {
                resolvedUrl = conn.getHeaderField("Location") ?: return@repeat
                conn.disconnect()
            } else {
                // Arrived at the real download endpoint — stream to disk
                val fileLength = conn.contentLength
                val outputFile = File(cacheDir, apkName)
                if (outputFile.exists()) outputFile.delete()

                conn.inputStream.use { input ->
                    outputFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var totalBytesRead = 0L
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            totalBytesRead += bytesRead
                            output.write(buffer, 0, bytesRead)
                            if (fileLength > 0) {
                                emit(totalBytesRead.toFloat() / fileLength.toFloat())
                            }
                        }
                    }
                }
                conn.disconnect()
                return@flow
            }
        }
    }.flowOn(Dispatchers.IO)

    fun installApk() {
        val apkFile = File(cacheDir, apkName)
        if (!apkFile.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            apkFile
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        context.startActivity(intent)
    }

    fun clearCache() {
        val apkFile = File(cacheDir, apkName)
        if (apkFile.exists()) {
            apkFile.delete()
        }
    }

    private fun isNewerVersion(remoteTag: String): Boolean {
        val remote = remoteTag.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val current = currentVersion.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until minOf(remote.size, current.size)) {
            if (remote[i] > current[i]) return true
            if (remote[i] < current[i]) return false
        }
        return remote.size > current.size
    }
}
