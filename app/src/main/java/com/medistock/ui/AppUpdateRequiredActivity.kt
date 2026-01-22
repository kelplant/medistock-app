package com.medistock.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medistock.R
import com.medistock.data.migration.MigrationManager
import com.medistock.util.AppUpdateManager
import com.medistock.util.DownloadState
import com.medistock.util.GitHubAsset
import com.medistock.util.UpdateCheckResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Écran affiché quand l'app est trop ancienne pour la base de données.
 *
 * Cet écran bloque l'utilisation de l'app et propose deux options:
 * 1. Télécharger la mise à jour depuis GitHub Releases
 * 2. Ouvrir le Play Store (si disponible)
 */
class AppUpdateRequiredActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_APP_VERSION = "app_version"
        const val EXTRA_MIN_REQUIRED = "min_required"
        const val EXTRA_DB_VERSION = "db_version"
    }

    private lateinit var updateManager: AppUpdateManager

    // UI elements
    private lateinit var btnDownloadGithub: Button
    private lateinit var btnUpdate: Button
    private lateinit var btnClose: Button
    private lateinit var downloadSection: LinearLayout
    private lateinit var progressDownload: ProgressBar
    private lateinit var tvDownloadStatus: TextView

    // State
    private var pendingApkFile: File? = null
    private var currentApkAsset: GitHubAsset? = null
    private var isDownloading = false

    private val installPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        // Check if permission was granted
        if (updateManager.canInstallUnknownApps()) {
            pendingApkFile?.let { apkFile ->
                installApk(apkFile)
            }
        } else {
            Toast.makeText(this, R.string.install_permission_required, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_update_required)

        updateManager = AppUpdateManager(this)

        // Récupérer les informations de version
        val appVersion = intent.getIntExtra(EXTRA_APP_VERSION, MigrationManager.APP_SCHEMA_VERSION)
        val minRequired = intent.getIntExtra(EXTRA_MIN_REQUIRED, 0)
        val dbVersion = intent.getIntExtra(EXTRA_DB_VERSION, 0)

        // Initialiser les vues
        val tvMessage = findViewById<TextView>(R.id.tvUpdateMessage)
        val tvVersionInfo = findViewById<TextView>(R.id.tvVersionInfo)
        btnDownloadGithub = findViewById(R.id.btnDownloadGithub)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnClose = findViewById(R.id.btnClose)
        downloadSection = findViewById(R.id.downloadSection)
        progressDownload = findViewById(R.id.progressDownload)
        tvDownloadStatus = findViewById(R.id.tvDownloadStatus)

        // Configurer les textes
        tvMessage.text = getString(R.string.update_required_message)
        tvVersionInfo.text = getString(
            R.string.update_version_info,
            appVersion,
            minRequired,
            dbVersion
        )

        // Bouton GitHub
        btnDownloadGithub.setOnClickListener {
            if (isDownloading) {
                // Already downloading, do nothing
                return@setOnClickListener
            }
            checkAndDownloadFromGitHub()
        }

        // Bouton Play Store
        btnUpdate.setOnClickListener {
            openPlayStore()
        }

        // Bouton Fermer
        btnClose.setOnClickListener {
            finishAffinity()
        }
    }

    private fun checkAndDownloadFromGitHub() {
        downloadSection.visibility = View.VISIBLE
        tvDownloadStatus.text = getString(R.string.download_checking)
        progressDownload.isIndeterminate = true
        btnDownloadGithub.isEnabled = false

        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) {
                updateManager.checkForUpdate()
            }

            when (result) {
                is UpdateCheckResult.UpdateAvailable -> {
                    currentApkAsset = result.apkAsset
                    tvDownloadStatus.text = getString(
                        R.string.download_available,
                        result.newVersion,
                        formatSize(result.apkAsset.size)
                    )
                    progressDownload.isIndeterminate = false
                    progressDownload.progress = 0

                    // Start download
                    startDownload(result.apkAsset)
                }

                is UpdateCheckResult.NoUpdateAvailable -> {
                    tvDownloadStatus.text = getString(R.string.download_no_update)
                    progressDownload.isIndeterminate = false
                    btnDownloadGithub.isEnabled = true
                }

                is UpdateCheckResult.Error -> {
                    tvDownloadStatus.text = getString(R.string.download_failed, result.message)
                    progressDownload.isIndeterminate = false
                    btnDownloadGithub.isEnabled = true
                }
            }
        }
    }

    private fun startDownload(apkAsset: GitHubAsset) {
        isDownloading = true

        lifecycleScope.launch {
            val apkFile = updateManager.downloadUpdate(apkAsset) { state ->
                lifecycleScope.launch(Dispatchers.Main) {
                    updateDownloadUI(state)
                }
            }

            isDownloading = false

            if (apkFile != null) {
                pendingApkFile = apkFile
                // Try to install
                requestInstallPermissionAndInstall(apkFile)
            }
        }
    }

    private fun updateDownloadUI(state: DownloadState) {
        when (state) {
            is DownloadState.Idle -> {
                progressDownload.isIndeterminate = true
            }

            is DownloadState.Downloading -> {
                progressDownload.isIndeterminate = false
                progressDownload.progress = state.progress
                tvDownloadStatus.text = getString(
                    R.string.download_progress,
                    state.progress,
                    formatSize(state.downloadedBytes),
                    formatSize(state.totalBytes)
                )
            }

            is DownloadState.Completed -> {
                progressDownload.progress = 100
                tvDownloadStatus.text = getString(R.string.download_complete)
                btnDownloadGithub.text = getString(R.string.install_button)
                btnDownloadGithub.isEnabled = true
                btnDownloadGithub.setOnClickListener {
                    state.apkFile.let { requestInstallPermissionAndInstall(it) }
                }
            }

            is DownloadState.Failed -> {
                tvDownloadStatus.text = getString(R.string.download_failed, state.error)
                btnDownloadGithub.isEnabled = true
            }
        }
    }

    private fun requestInstallPermissionAndInstall(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!updateManager.canInstallUnknownApps()) {
                // Need to request permission
                pendingApkFile = apkFile
                tvDownloadStatus.text = getString(R.string.install_permission_required)

                updateManager.getInstallPermissionIntent()?.let { intent ->
                    installPermissionLauncher.launch(intent)
                }
                return
            }
        }

        // Permission granted or not needed
        installApk(apkFile)
    }

    private fun installApk(apkFile: File) {
        try {
            tvDownloadStatus.text = getString(R.string.download_installing)
            updateManager.installApk(apkFile)
        } catch (e: Exception) {
            tvDownloadStatus.text = getString(R.string.download_failed, e.message)
            btnDownloadGithub.isEnabled = true
        }
    }

    private fun openPlayStore() {
        try {
            // Essayer d'ouvrir le Play Store directement
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$packageName")
                setPackage("com.android.vending")
            }
            startActivity(intent)
        } catch (e: Exception) {
            // Fallback vers le navigateur
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                }
                startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(this, "Play Store non disponible", Toast.LENGTH_SHORT).show()
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // Empêcher le retour arrière - l'utilisateur doit mettre à jour ou fermer
        // Ne rien faire
    }
}
