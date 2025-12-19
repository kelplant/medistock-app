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
import com.medistock.util.PasswordHasher
import com.medistock.util.PasswordMigration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

        // Initialize views
        editUsername = findViewById(R.id.editUsername)
        editPassword = findViewById(R.id.editPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvError = findViewById(R.id.tvError)

        // Disable login button during initialization
        btnLogin.isEnabled = false

        // IMPORTANT: Migrate passwords BEFORE checking login status
        lifecycleScope.launch {
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
                }
            }
        }

        btnLogin.setOnClickListener {
            login()
        }
    }

    private fun login() {
        val username = editUsername.text.toString().trim()
        val password = editPassword.text.toString()

        if (username.isEmpty() || password.isEmpty()) {
            showError("Please fill in all fields")
            return
        }

        lifecycleScope.launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    val u = db.userDao().getUserForAuth(username)

                    // Verify password using BCrypt
                    if (u != null && PasswordHasher.verifyPassword(password, u.password)) {
                        u
                    } else {
                        null
                    }
                }

                if (user != null) {
                    authManager.login(user)
                    navigateToHome()
                } else {
                    showError("Incorrect username or password")
                }
            } catch (e: Exception) {
                showError("Connection error: ${e.message}")
            }
        }
    }

    private suspend fun createDefaultAdminIfNeeded() {
        withContext(Dispatchers.IO) {
            val userCount = db.userDao().getAllUsers().size
            if (userCount == 0) {
                // Create default admin user
                val adminUser = User(
                    username = "admin",
                    password = PasswordHasher.hashPassword("admin"),
                    fullName = "Administrator",
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
