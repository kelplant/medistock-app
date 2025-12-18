package com.medistock.ui.auth

import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.util.AuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var editCurrentPassword: EditText
    private lateinit var editNewPassword: EditText
    private lateinit var editConfirmPassword: EditText
    private lateinit var btnSave: Button
    private lateinit var authManager: AuthManager
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Change Password"

        authManager = AuthManager.getInstance(this)
        db = AppDatabase.getInstance(this)

        editCurrentPassword = findViewById(R.id.editCurrentPassword)
        editNewPassword = findViewById(R.id.editNewPassword)
        editConfirmPassword = findViewById(R.id.editConfirmPassword)
        btnSave = findViewById(R.id.btnSavePassword)

        btnSave.setOnClickListener {
            changePassword()
        }
    }

    private fun changePassword() {
        val currentPassword = editCurrentPassword.text.toString()
        val newPassword = editNewPassword.text.toString()
        val confirmPassword = editConfirmPassword.text.toString()

        // Validation
        if (currentPassword.isEmpty()) {
            Toast.makeText(this, "Please enter current password", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.isEmpty()) {
            Toast.makeText(this, "Please enter new password", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword.length < 4) {
            Toast.makeText(this, "Password must be at least 4 characters", Toast.LENGTH_SHORT).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "New passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentPassword == newPassword) {
            Toast.makeText(this, "New password must be different from current password", Toast.LENGTH_SHORT).show()
            return
        }

        val username = authManager.getUsername()
        if (username.isEmpty()) {
            Toast.makeText(this, "No user logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                // Verify current password
                val user = withContext(Dispatchers.IO) {
                    db.userDao().authenticate(username, currentPassword)
                }

                if (user == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ChangePasswordActivity,
                            "Current password is incorrect",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@launch
                }

                // Update password
                val updatedUser = user.copy(
                    password = newPassword,
                    updatedAt = System.currentTimeMillis(),
                    updatedBy = username
                )

                withContext(Dispatchers.IO) {
                    db.userDao().updateUser(updatedUser)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ChangePasswordActivity,
                        "Password changed successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ChangePasswordActivity,
                        "Error changing password: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
