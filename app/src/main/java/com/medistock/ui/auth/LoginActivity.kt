package com.medistock.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.User
import com.medistock.data.entities.UserPermission
import com.medistock.ui.HomeActivity
import com.medistock.util.AuthManager
import com.medistock.util.Modules
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var editUsername: TextInputEditText
    private lateinit var editPassword: TextInputEditText
    private lateinit var btnLogin: Button
    private lateinit var tvError: TextView
    private lateinit var authManager: AuthManager
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize
        authManager = AuthManager.getInstance(this)
        db = AppDatabase.getInstance(this)

        // Check if already logged in
        if (authManager.isLoggedIn()) {
            navigateToHome()
            return
        }

        // Initialize views
        editUsername = findViewById(R.id.editUsername)
        editPassword = findViewById(R.id.editPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvError = findViewById(R.id.tvError)

        // Create default admin user if no users exist
        lifecycleScope.launch {
            createDefaultAdminIfNeeded()
        }

        btnLogin.setOnClickListener {
            login()
        }
    }

    private fun login() {
        val username = editUsername.text.toString().trim()
        val password = editPassword.text.toString()

        if (username.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs")
            return
        }

        lifecycleScope.launch {
            try {
                val user = db.userDao().authenticate(username, password)
                if (user != null) {
                    authManager.login(user)
                    navigateToHome()
                } else {
                    showError("Nom d'utilisateur ou mot de passe incorrect")
                }
            } catch (e: Exception) {
                showError("Erreur de connexion: ${e.message}")
            }
        }
    }

    private suspend fun createDefaultAdminIfNeeded() {
        val userCount = db.userDao().getAllUsers().size
        if (userCount == 0) {
            // Create default admin user
            val adminUser = User(
                username = "admin",
                password = "admin", // In production, this should be hashed
                fullName = "Administrateur",
                isAdmin = true,
                isActive = true,
                createdBy = "system",
                updatedBy = "system"
            )
            val userId = db.userDao().insertUser(adminUser)

            // Give admin all permissions (though admin check bypasses this)
            val modules = listOf(
                Modules.STOCK, Modules.SALES, Modules.PURCHASES,
                Modules.INVENTORY, Modules.ADMIN, Modules.PRODUCTS,
                Modules.SITES, Modules.CATEGORIES, Modules.USERS
            )
            val permissions = modules.map { module ->
                UserPermission(
                    userId = userId,
                    module = module,
                    canView = true,
                    canCreate = true,
                    canEdit = true,
                    canDelete = true,
                    createdBy = "system",
                    updatedBy = "system"
                )
            }
            db.userPermissionDao().insertPermissions(permissions)
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
    }

    private fun navigateToHome() {
        val intent = Intent(this, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
