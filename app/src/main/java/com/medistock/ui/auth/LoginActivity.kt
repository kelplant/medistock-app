package com.medistock.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.compatibility.CompatibilityResult
import com.medistock.data.remote.SupabaseAuthService
import com.medistock.data.remote.SupabaseAuthResult
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.data.sync.SyncManager
import com.medistock.ui.AppUpdateRequiredActivity
import com.medistock.ui.HomeActivity
import com.medistock.ui.admin.SupabaseConfigActivity
import com.medistock.shared.domain.auth.AuthResult
import com.medistock.shared.domain.auth.AuthService
import com.medistock.shared.domain.auth.OnlineFirstAuthResult
import com.medistock.shared.domain.auth.PasswordVerifier
import com.medistock.util.AuthManager
import com.medistock.util.DebugConfig
import com.medistock.util.NetworkStatus
import com.medistock.util.PasswordHasher
import com.medistock.util.PasswordMigration
import com.medistock.util.AppUpdateManager
import com.medistock.util.UpdateCheckResult
import com.medistock.shared.i18n.L
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Android implementation of PasswordVerifier using BCrypt.
 */
private object AndroidPasswordVerifier : PasswordVerifier {
    override fun verify(plainPassword: String, hashedPassword: String): Boolean {
        return PasswordHasher.verifyPassword(plainPassword, hashedPassword)
    }
}

class LoginActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "LoginActivity"
    }

    private lateinit var editUsername: TextInputEditText
    private lateinit var editPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvError: TextView
    private lateinit var tvRealtimeBadge: TextView
    private lateinit var btnSupabaseConfig: Button
    private lateinit var authManager: AuthManager
    private lateinit var sdk: MedistockSDK
    private lateinit var sharedAuthService: AuthService
    private lateinit var supabaseAuthService: SupabaseAuthService
    private var realtimeJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize
        authManager = AuthManager.getInstance(this)
        sdk = MedistockApplication.sdk
        sharedAuthService = sdk.createAuthService(AndroidPasswordVerifier)
        supabaseAuthService = SupabaseAuthService()

        // Initialize views
        editUsername = findViewById(R.id.editUsername)
        editPassword = findViewById(R.id.editPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvError = findViewById(R.id.tvError)
        tvRealtimeBadge = findViewById(R.id.tvRealtimeBadge)
        btnSupabaseConfig = findViewById(R.id.btnSupabaseConfig)

        // Disable login button during initialization
        btnLogin.isEnabled = false

        // IMPORTANT: Check app/DB compatibility first
        lifecycleScope.launch {
            // Wait for compatibility check to complete (max 5 seconds)
            checkAppCompatibility()

            // Migrate existing plain text passwords to hashed passwords
            PasswordMigration.migratePasswordsIfNeeded(this@LoginActivity)

            // Create default admin user if needed
            createDefaultAdminIfNeeded()

            // After migration, check if already logged in
            withContext(Dispatchers.Main) {
                if (authManager.isLoggedIn()) {
                    navigateToHome()
                } else {
                    // Enable login button
                    btnLogin.isEnabled = true

                    // Vérifier les mises à jour disponibles sur GitHub
                    checkForAppUpdates()
                }
            }
        }

        btnLogin.setOnClickListener {
            login()
        }

        btnSupabaseConfig.setOnClickListener {
            val intent = Intent(this, SupabaseConfigActivity::class.java)
            intent.putExtra(SupabaseConfigActivity.EXTRA_HIDE_KEY, true)
            startActivity(intent)
        }

        observeRealtimeStatus()
    }

    private fun login() {
        val username = editUsername.text.toString().trim()
        val password = editPassword.text.toString()

        if (username.isEmpty() || password.isEmpty()) {
            showError(L.strings.required)
            return
        }

        // Disable button and hide previous error
        btnLogin.isEnabled = false
        tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Check if this is a first-time login (no local users except system admin)
                val requiresOnlineAuth = withContext(Dispatchers.IO) {
                    isFirstTimeLogin()
                }

                if (requiresOnlineAuth) {
                    // First login REQUIRES network for online-first authentication
                    if (!NetworkStatus.isOnline(this@LoginActivity)) {
                        showError(L.strings.offlineMode)
                        return@launch
                    }

                    if (!SupabaseClientProvider.isConfigured(this@LoginActivity)) {
                        showError(L.strings.supabaseNotConfigured)
                        return@launch
                    }

                    // Use online-first authentication
                    val onlineResult = withContext(Dispatchers.IO) {
                        supabaseAuthService.authenticateOnlineFirst(username, password)
                    }

                    handleOnlineFirstAuthResult(onlineResult)
                    return@launch
                }

                // Not first login - use the standard flow
                loginWithStandardFlow(username, password)

            } catch (e: Exception) {
                showError("${L.strings.loginError}: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    btnLogin.isEnabled = true
                }
            }
        }
    }

    /**
     * Check if this is a first-time login.
     * First-time = no real users in local DB (only system admin marker or empty).
     */
    private suspend fun isFirstTimeLogin(): Boolean {
        val users = sdk.userRepository.getAll()

        // No users at all = first login
        if (users.isEmpty()) return true

        // Only system admin marker = first login
        if (users.all { it.createdBy == "LOCAL_SYSTEM_MARKER" }) return true

        // Has stored session = not first login
        if (authManager.hasValidSession()) return false

        return false
    }

    /**
     * Standard login flow for subsequent logins (when we have local users).
     */
    private suspend fun loginWithStandardFlow(username: String, password: String) {
        // Step 1: Look up user in local DB
        val localUser = withContext(Dispatchers.IO) {
            sharedAuthService.getUserByUsername(username)
        }

        if (localUser == null) {
            // User not in local DB - if online, try Edge Function
            if (NetworkStatus.isOnline(this@LoginActivity) &&
                SupabaseClientProvider.isConfigured(this@LoginActivity)) {
                val onlineResult = withContext(Dispatchers.IO) {
                    supabaseAuthService.authenticateOnlineFirst(username, password)
                }
                handleOnlineFirstAuthResult(onlineResult)
            } else {
                showError(L.strings.userNotFound)
            }
            return
        }

        // Check if user is active
        if (!localUser.isActive) {
            showError(L.strings.inactive)
            return
        }

        // Step 2: Try Supabase Auth if configured and online
        // Use online-first auth to get the correct UUID from Supabase
        // This handles cases where local and remote users have different UUIDs
        if (NetworkStatus.isOnline(this@LoginActivity) &&
            SupabaseClientProvider.isConfigured(this@LoginActivity)) {

            DebugConfig.d(TAG, "Using online-first auth for user: $username")
            val onlineResult = withContext(Dispatchers.IO) {
                supabaseAuthService.authenticateOnlineFirst(username, password)
            }

            DebugConfig.d(TAG, "Online-first auth result: $onlineResult")

            when (onlineResult) {
                is OnlineFirstAuthResult.Success -> {
                    DebugConfig.d(TAG, "Online-first auth SUCCESS")

                    // Store session tokens
                    val session = AuthManager.SupabaseSession(
                        accessToken = onlineResult.accessToken,
                        refreshToken = onlineResult.refreshToken,
                        expiresAt = onlineResult.expiresAt
                    )
                    authManager.loginWithSession(onlineResult.user, session)

                    // Update local user with correct UUID from Supabase
                    // BUT preserve the local password hash (Edge Function doesn't return it)
                    withContext(Dispatchers.IO) {
                        val existingUser = sdk.userRepository.getById(onlineResult.user.id)
                        val userToSave = if (existingUser != null && existingUser.password.isNotEmpty()) {
                            onlineResult.user.copy(password = existingUser.password)
                        } else {
                            onlineResult.user
                        }
                        sdk.userRepository.upsert(userToSave)
                    }

                    navigateToHome()
                    return
                }
                is OnlineFirstAuthResult.InvalidCredentials -> {
                    showError(L.strings.loginErrorInvalidCredentials)
                    return
                }
                is OnlineFirstAuthResult.UserNotFound -> {
                    // User exists locally but not in Supabase - try local auth
                    DebugConfig.d(TAG, "User not in Supabase, falling back to local auth")
                }
                is OnlineFirstAuthResult.UserInactive -> {
                    showError(L.strings.inactive)
                    return
                }
                is OnlineFirstAuthResult.NotConfigured,
                is OnlineFirstAuthResult.NetworkRequired,
                is OnlineFirstAuthResult.Error -> {
                    DebugConfig.d(TAG, "Online-first auth failed: ${(onlineResult as? OnlineFirstAuthResult.Error)?.message}")
                    // Fall through to local auth
                }
            }
        }

        // Step 3: Fall back to local authentication (offline mode)
        val localResult = withContext(Dispatchers.IO) {
            sharedAuthService.authenticate(username, password)
        }

        when (localResult) {
            is AuthResult.Success -> {
                authManager.login(localResult.user)
                navigateToHome()
            }
            is AuthResult.InvalidCredentials -> {
                showError(L.strings.loginErrorInvalidCredentials)
            }
            is AuthResult.UserNotFound -> {
                showError(L.strings.userNotFound)
            }
            is AuthResult.UserInactive -> {
                showError(L.strings.inactive)
            }
            is AuthResult.Error -> {
                showError("${L.strings.error}: ${localResult.message}")
            }
        }
    }

    /**
     * Handle the result from online-first authentication.
     * On success: saves user to local DB, stores session, triggers sync.
     */
    private suspend fun handleOnlineFirstAuthResult(result: OnlineFirstAuthResult) {
        when (result) {
            is OnlineFirstAuthResult.Success -> {
                // Save user to local database
                // BUT preserve the local password hash (Edge Function doesn't return it)
                withContext(Dispatchers.IO) {
                    val existingUser = sdk.userRepository.getById(result.user.id)
                    val userToSave = if (existingUser != null && existingUser.password.isNotEmpty()) {
                        result.user.copy(password = existingUser.password)
                    } else {
                        result.user
                    }
                    sdk.userRepository.upsert(userToSave)
                }

                // Store session tokens
                val session = AuthManager.SupabaseSession(
                    accessToken = result.accessToken,
                    refreshToken = result.refreshToken,
                    expiresAt = result.expiresAt
                )
                authManager.loginWithSession(result.user, session)

                // Trigger full sync to get all data
                withContext(Dispatchers.IO) {
                    try {
                        val syncManager = SyncManager(this@LoginActivity)
                        syncManager.fullSync(
                            onProgress = { DebugConfig.d(TAG, "First login sync: $it") },
                            onError = { entity, error ->
                                DebugConfig.w(TAG, "First login sync error on $entity: ${error.message}")
                            }
                        )
                    } catch (e: Exception) {
                        DebugConfig.w(TAG, "First login sync failed: ${e.message}")
                        // Continue anyway - user is logged in
                    }
                }

                navigateToHome()
            }
            is OnlineFirstAuthResult.InvalidCredentials -> {
                showError(L.strings.loginErrorInvalidCredentials)
            }
            is OnlineFirstAuthResult.UserNotFound -> {
                showError(L.strings.userNotFound)
            }
            is OnlineFirstAuthResult.UserInactive -> {
                showError(L.strings.inactive)
            }
            is OnlineFirstAuthResult.NetworkRequired -> {
                showError(L.strings.offlineMode)
            }
            is OnlineFirstAuthResult.NotConfigured -> {
                showError(L.strings.supabaseNotConfigured)
            }
            is OnlineFirstAuthResult.Error -> {
                showError("${L.strings.error}: ${result.message}")
            }
        }
        btnLogin.isEnabled = true
    }

    private suspend fun createDefaultAdminIfNeeded() {
        withContext(Dispatchers.IO) {
            // Use the shared DefaultAdminService
            val hashedPassword = PasswordHasher.hashPassword("admin")
            val currentTime = System.currentTimeMillis()
            sdk.defaultAdminService.createDefaultAdminIfNeeded(hashedPassword, currentTime)
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private suspend fun navigateToHome() {
        // Run pending migrations after successful login
        MedistockApplication.runMigrationsIfNeeded(this, "login")

        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    /**
     * Vérifie les mises à jour et la compatibilité app/DB.
     * Ordre:
     *   1. Attendre le flux de démarrage (GitHub check + migrations + compat check)
     *   2. Si mise à jour GitHub disponible → bloquer (écran de téléchargement)
     *   3. Si AppTooOld → bloquer (écran de téléchargement)
     *   4. Si DbTooOld après migrations → bloquer (écran DB incompatible)
     */
    private suspend fun checkAppCompatibility() {
        // Attendre que le flux de démarrage soit terminé (max 15 secondes)
        var waited = 0
        while (!MedistockApplication.startupFlowCompleted && waited < 15000) {
            delay(100)
            waited += 100
        }

        // 1. Vérifier si une mise à jour GitHub est disponible → BLOQUER
        val updateResult = MedistockApplication.latestUpdateResult
        if (updateResult != null) {
            withContext(Dispatchers.Main) {
                val intent = Intent(this@LoginActivity, AppUpdateRequiredActivity::class.java).apply {
                    putExtra(AppUpdateRequiredActivity.EXTRA_BLOCK_MODE, AppUpdateRequiredActivity.MODE_UPDATE)
                    putExtra(AppUpdateRequiredActivity.EXTRA_CURRENT_VERSION, updateResult.currentVersion)
                    putExtra(AppUpdateRequiredActivity.EXTRA_NEW_VERSION, updateResult.newVersion)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
            return
        }

        // 2. Vérifier la compatibilité schema (après migrations)
        val result = MedistockApplication.compatibilityResult

        when (result) {
            is CompatibilityResult.AppTooOld -> {
                // App trop ancienne → écran de mise à jour
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@LoginActivity, AppUpdateRequiredActivity::class.java).apply {
                        putExtra(AppUpdateRequiredActivity.EXTRA_BLOCK_MODE, AppUpdateRequiredActivity.MODE_UPDATE)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            }
            is CompatibilityResult.DbTooOld -> {
                // DB incompatible après migrations → écran DB incompatible
                withContext(Dispatchers.Main) {
                    val intent = Intent(this@LoginActivity, AppUpdateRequiredActivity::class.java).apply {
                        putExtra(AppUpdateRequiredActivity.EXTRA_BLOCK_MODE, AppUpdateRequiredActivity.MODE_DB_INCOMPATIBLE)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                }
            }
            else -> {
                // Compatible ou Unknown → on continue normalement
            }
        }
    }

    private fun observeRealtimeStatus() {
        realtimeJob?.cancel()

        if (!SupabaseClientProvider.isConfigured(this)) {
            updateRealtimeBadge(L.strings.supabaseNotConfigured, "#F44336", "#FFFFFF")
            return
        }

        val client = runCatching { SupabaseClientProvider.client }.getOrElse {
            updateRealtimeBadge(L.strings.supabaseNotConfigured, "#F44336", "#FFFFFF")
            return
        }

        realtimeJob = lifecycleScope.launch(Dispatchers.IO) {
            runCatching {
                withTimeoutOrNull(5000) {
                    client.realtime.connect()
                    client.realtime.status.firstOrNull { it == Realtime.Status.CONNECTED }
                }
            }

            client.realtime.status.collectLatest { status ->
                val strings = L.strings
                val (text, bg, fg) = when (status) {
                    Realtime.Status.CONNECTED -> Triple(strings.realtimeConnected, "#4CAF50", "#FFFFFF")
                    Realtime.Status.CONNECTING -> Triple(strings.syncing, "#FFC107", "#000000")
                    else -> Triple(strings.realtimeDisconnected, "#F44336", "#FFFFFF")
                }
                withContext(Dispatchers.Main) {
                    updateRealtimeBadge(text, bg, fg)
                }
            }
        }
    }

    private fun updateRealtimeBadge(text: String, bg: String, fg: String) {
        tvRealtimeBadge.text = text
        tvRealtimeBadge.setBackgroundColor(android.graphics.Color.parseColor(bg))
        tvRealtimeBadge.setTextColor(android.graphics.Color.parseColor(fg))
    }

    override fun onDestroy() {
        realtimeJob?.cancel()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        // Rafraîchir l'état du badge Realtime quand on revient sur cet écran
        // (par exemple après avoir configuré Supabase)
        observeRealtimeStatus()
    }

    /**
     * Vérifie si une mise à jour est disponible sur GitHub Releases.
     * Si oui, affiche un dialogue pour proposer à l'utilisateur de la télécharger.
     */
    private fun checkForAppUpdates() {
        lifecycleScope.launch {
            try {
                val updateManager = AppUpdateManager(this@LoginActivity)
                val result = withContext(Dispatchers.IO) {
                    updateManager.checkForUpdate()
                }

                when (result) {
                    is UpdateCheckResult.UpdateAvailable -> {
                        // Une mise à jour est disponible, afficher un dialogue
                        showUpdateAvailableDialog(
                            currentVersion = result.currentVersion,
                            newVersion = result.newVersion,
                            releaseNotes = result.release.body
                        )
                    }
                    is UpdateCheckResult.NoUpdateAvailable -> {
                        DebugConfig.d(TAG, "Application à jour")
                    }
                    is UpdateCheckResult.Error -> {
                        DebugConfig.w(TAG, "Impossible de vérifier les mises à jour: ${result.message}")
                        // Ne pas afficher d'erreur à l'utilisateur, cela peut être dû à l'absence de connexion
                    }
                }
            } catch (e: Exception) {
                DebugConfig.w(TAG, "Erreur lors de la vérification des mises à jour: ${e.message}")
                // Ne pas perturber l'utilisateur avec cette erreur
            }
        }
    }

    /**
     * Affiche un dialogue proposant à l'utilisateur de télécharger la mise à jour.
     */
    private fun showUpdateAvailableDialog(
        currentVersion: String,
        newVersion: String,
        releaseNotes: String?
    ) {
        val strings = L.strings
        AlertDialog.Builder(this)
            .setTitle(strings.updateAvailable)
            .setMessage(buildUpdateMessage(currentVersion, newVersion, releaseNotes))
            .setPositiveButton(strings.download) { _, _ ->
                // Rediriger vers l'écran de mise à jour
                navigateToUpdateScreen()
            }
            .setNegativeButton(strings.later) { dialog, _ ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    /**
     * Construit le message du dialogue de mise à jour.
     */
    private fun buildUpdateMessage(
        currentVersion: String,
        newVersion: String,
        releaseNotes: String?
    ): String {
        val strings = L.strings
        val message = StringBuilder()
        message.append("${strings.newVersionAvailable}\n\n")
        message.append("${strings.currentVersionLabel} : $currentVersion\n")
        message.append("${strings.newVersionLabel} : $newVersion\n")

        if (!releaseNotes.isNullOrBlank()) {
            message.append("\n${strings.whatsNew} :\n")
            // Limiter la longueur des notes de version pour le dialogue
            val shortNotes = if (releaseNotes.length > 200) {
                releaseNotes.take(200) + "..."
            } else {
                releaseNotes
            }
            message.append(shortNotes)
        }

        return message.toString()
    }

    /**
     * Redirige vers l'écran de téléchargement/installation de mise à jour.
     */
    private fun navigateToUpdateScreen() {
        val intent = Intent(this, AppUpdateRequiredActivity::class.java)
        startActivity(intent)
    }
}
