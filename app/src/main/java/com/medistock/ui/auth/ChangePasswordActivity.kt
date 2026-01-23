package com.medistock.ui.auth

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.validation.PasswordPolicy
import com.medistock.shared.domain.validation.PasswordPolicy.PasswordError
import com.medistock.shared.domain.validation.PasswordPolicy.PasswordStrength
import com.medistock.shared.i18n.LocalizationManager
import com.medistock.util.AuthManager
import com.medistock.util.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var editCurrentPassword: EditText
    private lateinit var editNewPassword: EditText
    private lateinit var editConfirmPassword: EditText
    private lateinit var btnSave: Button
    private lateinit var authManager: AuthManager
    private lateinit var sdk: MedistockSDK

    // Labels
    private lateinit var labelCurrentPassword: TextView
    private lateinit var labelNewPassword: TextView
    private lateinit var labelConfirmPassword: TextView

    // Password strength UI
    private lateinit var passwordStrengthContainer: LinearLayout
    private lateinit var progressPasswordStrength: ProgressBar
    private lateinit var labelPasswordStrength: TextView
    private lateinit var textPasswordStrength: TextView

    // Password requirements UI
    private lateinit var cardPasswordRequirements: CardView
    private lateinit var labelPasswordRequirements: TextView
    private lateinit var iconReqLength: ImageView
    private lateinit var iconReqUppercase: ImageView
    private lateinit var iconReqLowercase: ImageView
    private lateinit var iconReqDigit: ImageView
    private lateinit var iconReqSpecial: ImageView
    private lateinit var textReqLength: TextView
    private lateinit var textReqUppercase: TextView
    private lateinit var textReqLowercase: TextView
    private lateinit var textReqDigit: TextView
    private lateinit var textReqSpecial: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        val strings = LocalizationManager.strings
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = strings.changePassword

        authManager = AuthManager.getInstance(this)
        sdk = MedistockApplication.sdk

        initViews()
        applyLocalizedStrings()
        setupPasswordValidation()

        btnSave.setOnClickListener {
            changePassword()
        }
    }

    private fun initViews() {
        editCurrentPassword = findViewById(R.id.editCurrentPassword)
        editNewPassword = findViewById(R.id.editNewPassword)
        editConfirmPassword = findViewById(R.id.editConfirmPassword)
        btnSave = findViewById(R.id.btnSavePassword)

        // Labels
        labelCurrentPassword = findViewById(R.id.labelCurrentPassword)
        labelNewPassword = findViewById(R.id.labelNewPassword)
        labelConfirmPassword = findViewById(R.id.labelConfirmPassword)

        // Password strength
        passwordStrengthContainer = findViewById(R.id.passwordStrengthContainer)
        progressPasswordStrength = findViewById(R.id.progressPasswordStrength)
        labelPasswordStrength = findViewById(R.id.labelPasswordStrength)
        textPasswordStrength = findViewById(R.id.textPasswordStrength)

        // Password requirements
        cardPasswordRequirements = findViewById(R.id.cardPasswordRequirements)
        labelPasswordRequirements = findViewById(R.id.labelPasswordRequirements)
        iconReqLength = findViewById(R.id.iconReqLength)
        iconReqUppercase = findViewById(R.id.iconReqUppercase)
        iconReqLowercase = findViewById(R.id.iconReqLowercase)
        iconReqDigit = findViewById(R.id.iconReqDigit)
        iconReqSpecial = findViewById(R.id.iconReqSpecial)
        textReqLength = findViewById(R.id.textReqLength)
        textReqUppercase = findViewById(R.id.textReqUppercase)
        textReqLowercase = findViewById(R.id.textReqLowercase)
        textReqDigit = findViewById(R.id.textReqDigit)
        textReqSpecial = findViewById(R.id.textReqSpecial)
    }

    private fun applyLocalizedStrings() {
        val strings = LocalizationManager.strings

        labelCurrentPassword.text = strings.currentPassword
        labelNewPassword.text = strings.newPassword
        labelConfirmPassword.text = strings.confirmPassword
        btnSave.text = strings.changePassword

        // Password strength label
        labelPasswordStrength.text = strings.passwordStrength

        // Password requirements
        labelPasswordRequirements.text = strings.passwordRequirements
        textReqLength.text = strings.passwordMinLength
        textReqUppercase.text = strings.passwordNeedsUppercase
        textReqLowercase.text = strings.passwordNeedsLowercase
        textReqDigit.text = strings.passwordNeedsDigit
        textReqSpecial.text = strings.passwordNeedsSpecial
    }

    private fun setupPasswordValidation() {
        editNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val password = s?.toString() ?: ""
                updatePasswordStrengthUI(password)
                updateRequirementsUI(password)
            }
        })
    }

    private fun updatePasswordStrengthUI(password: String) {
        if (password.isEmpty()) {
            passwordStrengthContainer.visibility = View.GONE
            return
        }

        passwordStrengthContainer.visibility = View.VISIBLE

        val strength = PasswordPolicy.getStrength(password)
        val strings = LocalizationManager.strings

        // Update progress
        progressPasswordStrength.progress = strength.toProgress()

        // Update label
        textPasswordStrength.text = PasswordPolicy.getStrengthLabel(strength, strings)

        // Update color
        val rgb = strength.toRGB()
        val color = Color.rgb(rgb.first, rgb.second, rgb.third)
        textPasswordStrength.setTextColor(color)

        // Update progress bar color
        progressPasswordStrength.progressDrawable?.colorFilter =
            PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
    }

    private fun updateRequirementsUI(password: String) {
        val requirements = PasswordPolicy.checkRequirements(password)

        updateRequirementIcon(iconReqLength, requirements[PasswordError.TOO_SHORT] ?: false)
        updateRequirementIcon(iconReqUppercase, requirements[PasswordError.MISSING_UPPERCASE] ?: false)
        updateRequirementIcon(iconReqLowercase, requirements[PasswordError.MISSING_LOWERCASE] ?: false)
        updateRequirementIcon(iconReqDigit, requirements[PasswordError.MISSING_DIGIT] ?: false)
        updateRequirementIcon(iconReqSpecial, requirements[PasswordError.MISSING_SPECIAL] ?: false)
    }

    private fun updateRequirementIcon(icon: ImageView, met: Boolean) {
        if (met) {
            icon.setImageResource(R.drawable.ic_check_circle)
            icon.clearColorFilter()
        } else {
            icon.setImageResource(R.drawable.ic_circle_outline)
            icon.setColorFilter(
                ContextCompat.getColor(this, android.R.color.darker_gray),
                PorterDuff.Mode.SRC_IN
            )
        }
    }

    private fun changePassword() {
        val strings = LocalizationManager.strings
        val currentPassword = editCurrentPassword.text.toString()
        val newPassword = editNewPassword.text.toString()
        val confirmPassword = editConfirmPassword.text.toString()

        // Validation: current password required
        if (currentPassword.isEmpty()) {
            Toast.makeText(this, strings.fieldRequired.replace("{field}", strings.currentPassword), Toast.LENGTH_SHORT).show()
            return
        }

        // Validation: new password required
        if (newPassword.isEmpty()) {
            Toast.makeText(this, strings.fieldRequired.replace("{field}", strings.newPassword), Toast.LENGTH_SHORT).show()
            return
        }

        // Validation: password complexity
        val validationResult = PasswordPolicy.validate(newPassword)
        if (!validationResult.isValid) {
            // Show first error
            val firstError = validationResult.errors.firstOrNull()
            if (firstError != null) {
                Toast.makeText(this, PasswordPolicy.getErrorMessage(firstError, strings), Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Validation: passwords match
        if (newPassword != confirmPassword) {
            Toast.makeText(this, strings.passwordsDoNotMatch, Toast.LENGTH_SHORT).show()
            return
        }

        // Validation: new password different from current
        if (currentPassword == newPassword) {
            Toast.makeText(this, strings.passwordMustBeDifferent, Toast.LENGTH_SHORT).show()
            return
        }

        val username = authManager.getUsername()
        if (username.isEmpty()) {
            Toast.makeText(this, strings.userNotFound, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        lifecycleScope.launch {
            try {
                // Verify current password
                val user = withContext(Dispatchers.IO) {
                    val u = sdk.userRepository.getByUsername(username)
                    if (u != null && PasswordHasher.verifyPassword(currentPassword, u.password)) {
                        u
                    } else {
                        null
                    }
                }

                if (user == null) {
                    Toast.makeText(
                        this@ChangePasswordActivity,
                        strings.incorrectPassword,
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Update password with hashed version
                val currentTime = System.currentTimeMillis()
                withContext(Dispatchers.IO) {
                    sdk.userRepository.updatePassword(
                        userId = user.id,
                        password = PasswordHasher.hashPassword(newPassword),
                        updatedAt = currentTime,
                        updatedBy = username
                    )
                }

                Toast.makeText(
                    this@ChangePasswordActivity,
                    strings.passwordChangedSuccessfully,
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@ChangePasswordActivity,
                    "${strings.error}: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
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
