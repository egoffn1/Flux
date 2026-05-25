package com.fluxmusic.player.data.update

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class AppUpdateInfo(
    val latestVersion: String,
    val downloadUrl: String,
    val releaseNotes: String,
    val publishedAt: String
)

@Singleton
class UpdateChecker @Inject constructor() {

    companion object {
        private const val GITHUB_API = "https://api.github.com/repos/egoffn1/Flux/releases/latest"
        private const val TAG = "UpdateChecker"
    }

    suspend fun checkForUpdate(): Result<AppUpdateInfo> {
        return try {
            val response = fetchFromApi()
            val json = JSONObject(response)
            val tagName = json.getString("tag_name").removePrefix("v")
            val assets = json.getJSONArray("assets")
            var downloadUrl = ""
            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk")) {
                    downloadUrl = asset.getString("browser_download_url")
                    break
                }
            }
            val body = json.optString("body", "")
            val publishedAt = json.optString("published_at", "")

            if (downloadUrl.isEmpty()) {
                return Result.failure(Exception("No APK found in latest release"))
            }

            Result.success(
                AppUpdateInfo(
                    latestVersion = tagName,
                    downloadUrl = downloadUrl,
                    releaseNotes = body,
                    publishedAt = publishedAt
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for updates", e)
            Result.failure(e)
        }
    }

    private fun fetchFromApi(): String {
        val url = URL(GITHUB_API)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        return try {
            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
