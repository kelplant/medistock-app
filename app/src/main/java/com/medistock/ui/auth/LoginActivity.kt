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
import com.medistock.ui.AppUpdateRequiredActivity
import com.medistock.ui.HomeActivity
import com.medistock.ui.admin.SupabaseConfigActivity
import com.medistock.shared.domain.auth.AuthResult
import com.medistock.shared.domain.auth.AuthService
import com.medistock.shared.domain.auth.PasswordVerifier
import com.medistock.util.AuthManager
import com.medistock.util.PasswordHasher
import com.medistock.util.PasswordMigration
import com.medistock.util.AppUpdateManager
import com.medistock.util.UpdateCheckResult
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
            showError("Veuillez remplir tous les champs")
            return
        }

        // Disable button and hide previous error
        btnLogin.isEnabled = false
        tvError.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Step 1: Look up user in local DB to get UUID
                val localUser = withContext(Dispatchers.IO) {
                    sharedAuthService.getUserByUsername(username)
                }

                if (localUser == null) {
                    // User not in local DB, try Supabase Auth migration Edge Function
                    // This handles the case where user exists in Supabase but not locally
                    val migrationResult = withContext(Dispatchers.IO) {
                        supabaseAuthService.migrateUserToAuth(username, password)
                    }
                    handleSupabaseAuthResult(migrationResult)
                    return@launch
                }

                // Check if user is active locally
                if (!localUser.isActive) {
                    showError("Ce compte est désactivé")
                    return@launch
                }

                // Step 2: Try Supabase Auth if configured
                if (SupabaseClientProvider.isConfigured(this@LoginActivity)) {
                    val supabaseResult = withContext(Dispatchers.IO) {
                        supabaseAuthService.authenticateWithSupabaseAuth(
                            uuid = localUser.id,
                            password = password,
                            localUser = localUser
                        )
                    }

                    when (supabaseResult) {
                        is SupabaseAuthResult.Success -> {
                            // Login with Supabase Auth session
                            authManager.loginWithSession(supabaseResult.user, supabaseResult.session)
                            navigateToHome()
                            return@launch
                        }
                        is SupabaseAuthResult.NotConfigured -> {
                            // Fall back to local authentication
                            println("Supabase not configured, falling back to local auth")
                        }
                        is SupabaseAuthResult.InvalidCredentials -> {
                            showError("Nom d'utilisateur ou mot de passe incorrect")
                            return@launch
                        }
                        is SupabaseAuthResult.UserNotFound -> {
                            showError("Utilisateur non trouvé")
                            return@launch
                        }
                        is SupabaseAuthResult.UserInactive -> {
                            showError("Ce compte est désactivé")
                            return@launch
                        }
                        is SupabaseAuthResult.Error -> {
                            // Log the error but try local auth as fallback
                            println("Supabase Auth error: ${supabaseResult.message}, falling back to local auth")
                        }
                    }
                }

                // Step 3: Fall back to local authentication (offline mode)
                val localResult = withContext(Dispatchers.IO) {
                    sharedAuthService.authenticate(username, password)
                }

                when (localResult) {
                    is AuthResult.Success -> {
                        // Local auth only (no Supabase session)
                        authManager.login(localResult.user)
                        navigateToHome()
                    }
                    is AuthResult.InvalidCredentials -> {
                        showError("Nom d'utilisateur ou mot de passe incorrect")
                    }
                    is AuthResult.UserNotFound -> {
                        showError("Utilisateur non trouvé")
                    }
                    is AuthResult.UserInactive -> {
                        showError("Ce compte est désactivé")
                    }
                    is AuthResult.Error -> {
                        showError("Erreur: ${localResult.message}")
                    }
                }
            } catch (e: Exception) {
                showError("Erreur de connexion: ${e.message}")
            } finally {
                withContext(Dispatchers.Main) {
                    btnLogin.isEnabled = true
                }
            }
        }
    }

    /**
     * Handle the result from Supabase Auth operations
     */
    private suspend fun handleSupabaseAuthResult(result: SupabaseAuthResult) {
        when (result) {
            is SupabaseAuthResult.Success -> {
                authManager.loginWithSession(result.user, result.session)
                navigateToHome()
            }
            is SupabaseAuthResult.InvalidCredentials -> {
                showError("Nom d'utilisateur ou mot de passe incorrect")
            }
            is SupabaseAuthResult.UserNotFound -> {
                showError("Utilisateur non trouvé")
            }
            is SupabaseAuthResult.UserInactive -> {
                showError("Ce compte est désactivé")
            }
            is SupabaseAuthResult.NotConfigured -> {
                showError("Supabase n'est pas configuré")
            }
            is SupabaseAuthResult.Error -> {
                showError("Erreur: ${result.message}")
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
     * Vérifie la compatibilité app/DB.
     * Attend que la vérification soit terminée (max 5 secondes).
     * Si l'app est trop ancienne, redirige vers l'écran de mise à jour.
     */
    private suspend fun checkAppCompatibility() {
        // Attendre que la vérification soit terminée (l'Application la fait en background)
        var waited = 0
        while (MedistockApplication.compatibilityResult == null && waited < 5000) {
            delay(100)
            waited += 100
        }

        val result = MedistockApplication.compatibilityResult

        if (result is CompatibilityResult.AppTooOld) {
            // Rediriger vers l'écran de mise à jour requise
            withContext(Dispatchers.Main) {
                val intent = Intent(this@LoginActivity, AppUpdateRequiredActivity::class.java).apply {
                    putExtra(AppUpdateRequiredActivity.EXTRA_APP_VERSION, result.appVersion)
                    putExtra(AppUpdateRequiredActivity.EXTRA_MIN_REQUIRED, result.minRequired)
                    putExtra(AppUpdateRequiredActivity.EXTRA_DB_VERSION, result.dbVersion)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                finish()
            }
        }
        // Si Compatible ou Unknown, on continue normalement
    }

    private fun observeRealtimeStatus() {
        realtimeJob?.cancel()

        if (!SupabaseClientProvider.isConfigured(this)) {
            updateRealtimeBadge("Realtime non configuré", "#F44336", "#FFFFFF")
            return
        }

        val client = runCatching { SupabaseClientProvider.client }.getOrElse {
            updateRealtimeBadge("Client Supabase non initialisé", "#F44336", "#FFFFFF")
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
                val (text, bg, fg) = when (status) {
                    Realtime.Status.CONNECTED -> Triple("Realtime connecté", "#4CAF50", "#FFFFFF")
                    Realtime.Status.CONNECTING -> Triple("Connexion Realtime...", "#FFC107", "#000000")
                    else -> Triple("Realtime déconnecté", "#F44336", "#FFFFFF")
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
                        println("✅ Application à jour")
                    }
                    is UpdateCheckResult.Error -> {
                        println("⚠️ Impossible de vérifier les mises à jour: ${result.message}")
                        // Ne pas afficher d'erreur à l'utilisateur, cela peut être dû à l'absence de connexion
                    }
                }
            } catch (e: Exception) {
                println("⚠️ Erreur lors de la vérification des mises à jour: ${e.message}")
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
        AlertDialog.Builder(this)
            .setTitle("Mise à jour disponible")
            .setMessage(buildUpdateMessage(currentVersion, newVersion, releaseNotes))
            .setPositiveButton("Télécharger") { _, _ ->
                // Rediriger vers l'écran de mise à jour
                navigateToUpdateScreen()
            }
            .setNegativeButton("Plus tard") { dialog, _ ->
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
        val message = StringBuilder()
        message.append("Une nouvelle version de MediStock est disponible.\n\n")
        message.append("Version actuelle : $currentVersion\n")
        message.append("Nouvelle version : $newVersion\n")

        if (!releaseNotes.isNullOrBlank()) {
            message.append("\nNouveautés :\n")
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
