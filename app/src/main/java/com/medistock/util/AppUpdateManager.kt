package com.medistock.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

/**
 * Informations sur une release GitHub
 */
@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String,
    val name: String,
    val body: String? = null,
    @SerialName("html_url")
    val htmlUrl: String,
    val assets: List<GitHubAsset> = emptyList(),
    val prerelease: Boolean = false,
    val draft: Boolean = false
)

@Serializable
data class GitHubAsset(
    val name: String,
    @SerialName("browser_download_url")
    val downloadUrl: String,
    val size: Long,
    @SerialName("content_type")
    val contentType: String
)

/**
 * Résultat de la vérification de mise à jour
 */
sealed class UpdateCheckResult {
    data class UpdateAvailable(
        val release: GitHubRelease,
        val apkAsset: GitHubAsset,
        val currentVersion: String,
        val newVersion: String
    ) : UpdateCheckResult()

    object NoUpdateAvailable : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}

/**
 * État du téléchargement
 */
sealed class DownloadState {
    object Idle : DownloadState()
    data class Downloading(val progress: Int, val downloadedBytes: Long, val totalBytes: Long) : DownloadState()
    data class Completed(val apkFile: File) : DownloadState()
    data class Failed(val error: String) : DownloadState()
}

/**
 * Gestionnaire de mise à jour de l'application via GitHub Releases.
 *
 * Configuration requise:
 * 1. Définir GITHUB_OWNER et GITHUB_REPO
 * 2. Signer l'APK avec la même clé de release
 * 3. Uploader l'APK sur GitHub Releases avec le nom: medistock-vX.Y.Z.apk
 *
 * Le tag de la release doit suivre le format: vX.Y.Z (ex: v1.2.3)
 */
class AppUpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "AppUpdateManager"

        // Configuration GitHub - À modifier selon votre repo
        const val GITHUB_OWNER = "kelplant"
        const val GITHUB_REPO = "medistock-app"

        // API GitHub
        private const val GITHUB_API_BASE = "https://api.github.com"
        private const val RELEASES_ENDPOINT = "/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

        // Nom du fichier APK attendu (pattern)
        private const val APK_NAME_PATTERN = "medistock"
        private const val APK_EXTENSION = ".apk"

        // Dossier de téléchargement
        private const val DOWNLOAD_FOLDER = "updates"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Vérifie si une mise à jour est disponible sur GitHub Releases.
     *
     * @return UpdateCheckResult indiquant si une mise à jour est disponible
     */
    suspend fun checkForUpdate(): UpdateCheckResult = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersionName()
            println("📱 Version actuelle: $currentVersion")

            // Récupérer la dernière release
            val release = fetchLatestRelease()

            if (release == null) {
                return@withContext UpdateCheckResult.Error("Impossible de récupérer les informations de release")
            }

            if (release.draft || release.prerelease) {
                println("⏭️ Release ignorée (draft ou prerelease)")
                return@withContext UpdateCheckResult.NoUpdateAvailable
            }

            // Extraire la version du tag (format: vX.Y.Z -> X.Y.Z)
            val newVersion = release.tagName.removePrefix("v")
            println("🆕 Dernière version disponible: $newVersion")

            // Comparer les versions
            if (!isNewerVersion(currentVersion, newVersion)) {
                println("✅ L'application est à jour")
                return@withContext UpdateCheckResult.NoUpdateAvailable
            }

            // Chercher l'APK dans les assets
            val apkAsset = release.assets.find {
                it.name.contains(APK_NAME_PATTERN, ignoreCase = true) &&
                it.name.endsWith(APK_EXTENSION, ignoreCase = true)
            }

            if (apkAsset == null) {
                return@withContext UpdateCheckResult.Error("Aucun APK trouvé dans la release")
            }

            println("📦 APK trouvé: ${apkAsset.name} (${formatSize(apkAsset.size)})")

            UpdateCheckResult.UpdateAvailable(
                release = release,
                apkAsset = apkAsset,
                currentVersion = currentVersion,
                newVersion = newVersion
            )
        } catch (e: Exception) {
            println("❌ Erreur lors de la vérification: ${e.message}")
            UpdateCheckResult.Error(e.message ?: "Erreur inconnue")
        }
    }

    /**
     * Télécharge l'APK de mise à jour via DownloadManager.
     *
     * @param apkAsset L'asset APK à télécharger
     * @param onProgress Callback appelé avec l'état du téléchargement
     */
    suspend fun downloadUpdate(
        apkAsset: GitHubAsset,
        onProgress: (DownloadState) -> Unit
    ): File? = withContext(Dispatchers.IO) {
        try {
            onProgress(DownloadState.Downloading(0, 0, apkAsset.size))

            // Préparer le dossier de téléchargement
            val downloadDir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), DOWNLOAD_FOLDER)
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            // Supprimer les anciens APK
            downloadDir.listFiles()?.forEach { it.delete() }

            val apkFile = File(downloadDir, apkAsset.name)

            // Télécharger avec DownloadManager pour une meilleure UX
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

            val request = DownloadManager.Request(Uri.parse(apkAsset.downloadUrl)).apply {
                setTitle("Mise à jour Medistock")
                setDescription("Téléchargement de ${apkAsset.name}")
                setDestinationUri(Uri.fromFile(apkFile))
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(false)
            }

            val downloadId = downloadManager.enqueue(request)

            // Attendre la fin du téléchargement
            val result = waitForDownload(downloadManager, downloadId, apkAsset.size, onProgress)

            if (result && apkFile.exists()) {
                println("✅ Téléchargement terminé: ${apkFile.absolutePath}")
                onProgress(DownloadState.Completed(apkFile))
                apkFile
            } else {
                onProgress(DownloadState.Failed("Échec du téléchargement"))
                null
            }
        } catch (e: Exception) {
            println("❌ Erreur de téléchargement: ${e.message}")
            onProgress(DownloadState.Failed(e.message ?: "Erreur de téléchargement"))
            null
        }
    }

    /**
     * Lance l'installation de l'APK téléchargé.
     *
     * @param apkFile Le fichier APK à installer
     */
    fun installApk(apkFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // Android 7+ : utiliser FileProvider
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        apkFile
                    )
                } else {
                    Uri.fromFile(apkFile)
                }

                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            println("❌ Erreur lors de l'installation: ${e.message}")
            throw e
        }
    }

    /**
     * Vérifie si la permission d'installer des APK inconnus est accordée.
     * Nécessaire pour Android 8+.
     */
    fun canInstallUnknownApps(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Crée un intent pour demander la permission d'installer des APK inconnus.
     */
    fun getInstallPermissionIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}")
            )
        } else {
            null
        }
    }

    // --- Fonctions privées ---

    private fun getCurrentVersionName(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun fetchLatestRelease(): GitHubRelease? {
        val url = URL("$GITHUB_API_BASE$RELEASES_ENDPOINT")
        val connection = url.openConnection() as HttpURLConnection

        return try {
            connection.apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github.v3+json")
                setRequestProperty("User-Agent", "Medistock-Android")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().readText()
                json.decodeFromString<GitHubRelease>(response)
            } else {
                println("⚠️ GitHub API répondu: ${connection.responseCode}")
                null
            }
        } finally {
            connection.disconnect()
        }
    }

    /**
     * Compare deux versions au format X.Y.Z
     * @return true si newVersion est plus récente que currentVersion
     */
    private fun isNewerVersion(currentVersion: String, newVersion: String): Boolean {
        val current = parseVersion(currentVersion)
        val new = parseVersion(newVersion)

        for (i in 0 until maxOf(current.size, new.size)) {
            val currentPart = current.getOrElse(i) { 0 }
            val newPart = new.getOrElse(i) { 0 }

            if (newPart > currentPart) return true
            if (newPart < currentPart) return false
        }

        return false
    }

    private fun parseVersion(version: String): List<Int> {
        return version
            .split(".")
            .mapNotNull { it.toIntOrNull() }
    }

    private suspend fun waitForDownload(
        downloadManager: DownloadManager,
        downloadId: Long,
        totalSize: Long,
        onProgress: (DownloadState) -> Unit
    ): Boolean = suspendCancellableCoroutine { continuation ->
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    context.unregisterReceiver(this)

                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)

                    if (cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = cursor.getInt(statusIndex)
                        cursor.close()

                        if (continuation.isActive) {
                            continuation.resume(status == DownloadManager.STATUS_SUCCESSFUL)
                        }
                    } else {
                        cursor.close()
                        if (continuation.isActive) {
                            continuation.resume(false)
                        }
                    }
                }
            }
        }

        // Enregistrer le receiver pour la fin du téléchargement
        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }

        // Thread pour suivre la progression
        Thread {
            var lastProgress = 0
            while (continuation.isActive) {
                try {
                    val query = DownloadManager.Query().setFilterById(downloadId)
                    val cursor = downloadManager.query(query)

                    if (cursor.moveToFirst()) {
                        val bytesDownloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val bytesTotalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)

                        val bytesDownloaded = cursor.getLong(bytesDownloadedIndex)
                        val bytesTotal = cursor.getLong(bytesTotalIndex).takeIf { it > 0 } ?: totalSize
                        val status = cursor.getInt(statusIndex)

                        cursor.close()

                        val progress = if (bytesTotal > 0) {
                            ((bytesDownloaded * 100) / bytesTotal).toInt()
                        } else 0

                        if (progress != lastProgress) {
                            lastProgress = progress
                            onProgress(DownloadState.Downloading(progress, bytesDownloaded, bytesTotal))
                        }

                        if (status == DownloadManager.STATUS_SUCCESSFUL ||
                            status == DownloadManager.STATUS_FAILED) {
                            break
                        }
                    } else {
                        cursor.close()
                        break
                    }

                    Thread.sleep(500)
                } catch (e: Exception) {
                    break
                }
            }
        }.start()

        continuation.invokeOnCancellation {
            try {
                context.unregisterReceiver(receiver)
                downloadManager.remove(downloadId)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private fun formatSize(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}
