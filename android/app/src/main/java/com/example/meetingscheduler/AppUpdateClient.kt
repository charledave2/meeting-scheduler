package com.example.meetingscheduler

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

internal data class AppUpdateDownloadProgress(val percent: Int?, val complete: Boolean, val failed: Boolean)

internal object AppUpdateClient {
    private const val RELEASE_API = "https://api.github.com/repos/charledave2/meeting-scheduler/releases/latest"
    private const val APK_NAME = "app-release.apk"

    suspend fun checkForUpdate(currentVersion: String): AvailableAppUpdate? = withContext(Dispatchers.IO) {
        val connection = (URL(RELEASE_API).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Meeting-Scheduler-Android")
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val release = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val version = release.optString("tag_name").removePrefix("v").trim()
            if (version.isBlank() || compareVersions(version, currentVersion) <= 0) return@withContext null
            val assets = release.optJSONArray("assets") ?: return@withContext null
            val apkUrl = (0 until assets.length())
                .mapNotNull { assets.optJSONObject(it) }
                .firstOrNull { it.optString("name") == APK_NAME }
                ?.optString("browser_download_url")
                ?.takeIf { it.startsWith("https://github.com/") }
                ?: return@withContext null
            val notes = release.optString("body").lineSequence()
                .filter { it.isNotBlank() }
                .take(6)
                .joinToString("\n")
            AvailableAppUpdate(version, apkUrl, notes)
        } finally {
            connection.disconnect()
        }
    }

    fun download(context: Context, apkUrl: String): Long {
        val uri = Uri.parse(apkUrl)
        require(uri.scheme == "https" && uri.host == "github.com") { "Hindi valid ang APK download link." }
        val request = DownloadManager.Request(uri)
            .setTitle("Meeting Scheduler update")
            .setDescription("Dina-download ang bagong bersyon…")
            .setMimeType("application/vnd.android.package-archive")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, APK_NAME)
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        return manager.enqueue(request)
    }

    suspend fun downloadProgress(context: Context, downloadId: Long): AppUpdateDownloadProgress? = withContext(Dispatchers.IO) {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val cursor = manager.query(DownloadManager.Query().setFilterById(downloadId))
        cursor.use {
            if (!it.moveToFirst()) return@withContext AppUpdateDownloadProgress(null, complete = false, failed = true)
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            val downloaded = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            val total = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
            val percent = if (total > 0L) ((downloaded * 100L) / total).toInt().coerceIn(0, 100) else null
            AppUpdateDownloadProgress(
                percent = percent,
                complete = status == DownloadManager.STATUS_SUCCESSFUL,
                failed = status == DownloadManager.STATUS_FAILED
            )
        }
    }

    fun canInstallPackages(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallSettings(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun install(context: Context, downloadId: Long) {
        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val apkUri = checkNotNull(manager.getUriForDownloadedFile(downloadId)) { "Hindi makita ang na-download na APK." }
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(apkUri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun compareVersions(left: String, right: String): Int {
        val a = Regex("\\d+").findAll(left).mapNotNull { it.value.toIntOrNull() }.toList()
        val b = Regex("\\d+").findAll(right).mapNotNull { it.value.toIntOrNull() }.toList()
        for (index in 0 until maxOf(a.size, b.size)) {
            val comparison = (a.getOrNull(index) ?: 0).compareTo(b.getOrNull(index) ?: 0)
            if (comparison != 0) return comparison
        }
        return 0
    }
}
