package com.heliactyl.bororwjabbilai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.Keep
import androidx.core.content.FileProvider
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

@Keep
data class GitHubAsset(
    @SerializedName("name") val name: String,
    @SerializedName("browser_download_url") val downloadUrl: String,
)

@Keep
data class GitHubRelease(
    @SerializedName("tag_name") val tagName: String,
    @SerializedName("body") val body: String,
    @SerializedName("html_url") val htmlUrl: String,
    @SerializedName("assets") val assets: List<GitHubAsset>,
)

class UpdateManager(private val context: Context) {
    private val gson = Gson()

    private val currentVersion: String
        get() = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (_: Exception) {
            "0.0.0"
        }

    private val repoUrl = "https://api.github.com/repos/Badmaneers/bororwjabbilai/releases/latest"
    private val cacheDir = context.cacheDir
    private val apkName = "update.apk"

    suspend fun checkForUpdate(): GitHubRelease? {
        return withContext(Dispatchers.IO) {
            try {
                val url = URL(repoUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "BoroRwjabBilai-App")
                connection.connectTimeout = 10000
                connection.readTimeout = 10000

                if (connection.responseCode == 200) {
                    val json = connection.inputStream.bufferedReader().use { it.readText() }
                    val release = gson.fromJson(json, GitHubRelease::class.java)
                    
                    if (isNewerVersion(release.tagName)) {
                        release
                    } else {
                        null
                    }
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                throw e
            }
        }
    }

    fun downloadApk(downloadUrl: String): Flow<Float> = flow {
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

    fun canRequestInstall(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            context.packageManager.canRequestPackageInstalls()

    fun hasDownloadedApk(): Boolean = File(cacheDir, apkName).exists()

    /**
     * Launches the package installer for the downloaded APK.
     * Returns false when install permission is missing — Settings is opened
     * instead so the caller can keep its UI and retry on return.
     */
    fun installApk(): Boolean {
        val apkFile = File(cacheDir, apkName)
        if (!apkFile.exists()) return true

        if (!canRequestInstall()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            ).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            Toast.makeText(context, "Please enable 'Install unknown apps' permission and try again", Toast.LENGTH_LONG).show()
            return false
        }

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
        return true
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
