package com.fluxmusic.player.data.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUpdater @Inject constructor(
    @ApplicationContext private val context: Context,
    private val updateChecker: UpdateChecker
) {
    companion object {
        private const val TAG = "AppUpdater"
        private const val FILE_PROVIDER_AUTHORITY = "com.fluxmusic.player.fileprovider"
        private const val DOWNLOAD_FILE_NAME = "flux-update.apk"
    }

    private val _downloadProgress = MutableLiveData<Int>()
    val downloadProgress: LiveData<Int> = _downloadProgress

    private val _downloadStatus = MutableLiveData<String>()
    val downloadStatus: LiveData<String> = _downloadStatus

    private var downloadId: Long = -1L
    private var downloadManager: DownloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private var receiverRegistered = false
    private var scope: CoroutineScope? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            if (id == downloadId) {
                scope?.launch { queryDownloadProgress() }
            }
        }
    }

    fun checkAndUpdate(onResult: (Result<AppUpdateInfo?>) -> Unit) {
        _downloadStatus.postValue("Checking for updates...")
        scope?.cancel()
        scope = CoroutineScope(Dispatchers.IO)
        scope?.launch {
            val result = updateChecker.checkForUpdate()
            result.onSuccess { info ->
                val currentVersion = getCurrentVersion()
                if (isNewerVersion(info.latestVersion, currentVersion)) {
                    withContext(Dispatchers.Main) {
                        onResult(Result.success(info))
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _downloadStatus.postValue("You have the latest version ($currentVersion)")
                        onResult(Result.success(null))
                    }
                }
            }.onFailure { error ->
                withContext(Dispatchers.Main) {
                    _downloadStatus.postValue("Error: ${error.message}")
                    onResult(Result.failure(error))
                }
            }
        }
    }

    fun downloadAndInstall(updateInfo: AppUpdateInfo) {
        val existingFile = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), DOWNLOAD_FILE_NAME)
        existingFile.delete()

        val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl)).apply {
            setTitle("Flux Music Update")
            setDescription("Downloading ${updateInfo.latestVersion}")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, DOWNLOAD_FILE_NAME)
            setMimeType("application/vnd.android.package-archive")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                setRequiresCharging(false)
            }
        }

        if (!receiverRegistered) {
            context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
            receiverRegistered = true
        }

        downloadId = downloadManager.enqueue(request)
        _downloadStatus.postValue("Downloading...")

        scope?.launch {
            var completed = false
            while (!completed) {
                delay(1000)
                completed = queryDownloadProgress()
            }
        }
    }

    private suspend fun queryDownloadProgress(): Boolean {
        if (downloadId < 0) return true
        val query = DownloadManager.Query().setFilterById(downloadId)
        var cursor: Cursor? = null
        return try {
            cursor = downloadManager.query(query)
            if (cursor.moveToFirst()) {
                val status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val bytesDownloaded = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val totalBytes = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                if (totalBytes > 0) {
                    val progress = (bytesDownloaded * 100 / totalBytes).toInt()
                    withContext(Dispatchers.Main) {
                        _downloadProgress.postValue(progress)
                        _downloadStatus.postValue("Downloading... $progress%")
                    }
                }

                when (status) {
                    DownloadManager.STATUS_SUCCESSFUL -> {
                        withContext(Dispatchers.Main) {
                            _downloadStatus.postValue("Download complete")
                        }
                        installApk()
                        true
                    }
                    DownloadManager.STATUS_FAILED -> {
                        val reason = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                        withContext(Dispatchers.Main) {
                            _downloadStatus.postValue("Download failed: $reason")
                        }
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying download", e)
            true
        } finally {
            cursor?.close()
        }
    }

    private fun installApk() {
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), DOWNLOAD_FILE_NAME)
        if (!file.exists()) {
            _downloadStatus.postValue("File not found for installation")
            return
        }
        val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(intent)
        _downloadStatus.postValue("Installing...")
    }

    private fun getCurrentVersion(): String {
        return try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"
        } catch (e: Exception) {
            "0.0.0"
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").map { it.toIntOrNull() ?: 0 }
        val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    fun cleanup() {
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}
            receiverRegistered = false
        }
    }
}
