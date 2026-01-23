package com.medistock.ui.user

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.User
import com.medistock.shared.domain.model.UserPermission
import com.medistock.shared.domain.model.Module
import com.medistock.shared.domain.validation.PasswordPolicy
import com.medistock.shared.domain.validation.PasswordPolicy.PasswordError
import com.medistock.shared.i18n.LocalizationManager
import com.medistock.util.AuthManager
import com.medistock.util.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class UserAddEditActivity : AppCompatActivity() {

    private lateinit var editFullName: EditText
    private lateinit var editUsername: EditText
    private lateinit var editPassword: EditText
    private lateinit var checkIsAdmin: CheckBox
    private lateinit var checkIsActive: CheckBox
    private lateinit var btnSaveUser: Button
    private lateinit var btnToggleActive: Button
    private lateinit var tvAdminNote: TextView
    private lateinit var permissionsContainer: LinearLayout
    private lateinit var tvUserInfoTitle: TextView
    private lateinit var tvPermissionsTitle: TextView
    private lateinit var labelPassword: TextView

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

    private lateinit var sdk: MedistockSDK
    private lateinit var authManager: AuthManager
    private var userId: String? = null
    private var isEditMode = false
    private var currentUserIsActive = true

    private val permissionViews = mutableMapOf<String, PermissionCheckboxes>()

    private val modules = listOf(
        Module.STOCK to "Stock",
        Module.SALES to "Sales",
        Module.PURCHASES to "Purchases",
        Module.INVENTORY to "Inventory",
        Module.PRODUCTS to "Products",
        Module.SITES to "Sites",
        Module.CATEGORIES to "Categories",
        Module.ADMIN to "Administration"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = AuthManager.getInstance(this)
        val strings = LocalizationManager.strings

        // Check if user is admin
        if (!authManager.isAdmin()) {
            Toast.makeText(this, strings.permissions, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_user_add_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sdk = MedistockApplication.sdk

        initViews()
        applyLocalizedStrings()
        setupPermissionsUI()
        setupPasswordValidation()

        // Check admin checkbox behavior
        checkIsAdmin.setOnCheckedChangeListener { _, isChecked ->
            updatePermissionsVisibility(isChecked)
        }

        // Check if editing existing user
        userId = intent.getStringExtra("USER_ID")?.takeIf { it.isNotBlank() }
        if (userId != null) {
            isEditMode = true
            supportActionBar?.title = strings.editUser
            loadUser()
            btnToggleActive.visibility = View.VISIBLE
            // Hide password requirements card in edit mode (password is optional)
            cardPasswordRequirements.visibility = View.GONE
        } else {
            supportActionBar?.title = strings.addUser
        }

        btnSaveUser.setOnClickListener { saveUser() }
        btnToggleActive.setOnClickListener { toggleUserActiveStatus() }
    }

    private fun initViews() {
        editFullName = findViewById(R.id.editFullName)
        editUsername = findViewById(R.id.editUsername)
        editPassword = findViewById(R.id.editPassword)
        checkIsAdmin = findViewById(R.id.checkIsAdmin)
        checkIsActive = findViewById(R.id.checkIsActive)
        btnSaveUser = findViewById(R.id.btnSaveUser)
        btnToggleActive = findViewById(R.id.btnDeleteUser)
        tvAdminNote = findViewById(R.id.tvAdminNote)
        permissionsContainer = findViewById(R.id.permissionsContainer)
        tvUserInfoTitle = findViewById(R.id.tvUserInfoTitle)
        tvPermissionsTitle = findViewById(R.id.tvPermissionsTitle)
        labelPassword = findViewById(R.id.labelPassword)

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

        tvUserInfoTitle.text = strings.information
        editFullName.hint = strings.fullName
        editUsername.hint = strings.username
        labelPassword.text = strings.password
        editPassword.hint = strings.password
        checkIsAdmin.text = strings.admin
        checkIsActive.text = strings.active
        tvPermissionsTitle.text = strings.permissions
        btnSaveUser.text = strings.save

        // Password strength
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
        editPassword.addTextChangedListener(object : TextWatcher {
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

    private fun setupPermissionsUI() {
        val strings = LocalizationManager.strings

        // Module name translations
        val moduleNames = mapOf(
            Module.STOCK to strings.stock,
            Module.SALES to strings.sales,
            Module.PURCHASES to strings.purchases,
            Module.INVENTORY to strings.inventory,
            Module.PRODUCTS to strings.products,
            Module.SITES to strings.sites,
            Module.CATEGORIES to strings.categories,
            Module.ADMIN to strings.admin
        )

        modules.forEach { (moduleKey, _) ->
            val permView = LayoutInflater.from(this).inflate(R.layout.item_permission, permissionsContainer, false)

            val tvModuleName = permView.findViewById<TextView>(R.id.tvModuleName)
            val checkSelectAll = permView.findViewById<CheckBox>(R.id.btnSelectAll)
            val checkCanView = permView.findViewById<CheckBox>(R.id.checkCanView)
            val checkCanCreate = permView.findViewById<CheckBox>(R.id.checkCanCreate)
            val checkCanEdit = permView.findViewById<CheckBox>(R.id.checkCanEdit)
            val checkCanDelete = permView.findViewById<CheckBox>(R.id.checkCanDelete)

            tvModuleName.text = moduleNames[moduleKey] ?: moduleKey.name
            checkCanView.text = strings.canView
            checkCanCreate.text = strings.canCreate
            checkCanEdit.text = strings.canEdit
            checkCanDelete.text = strings.canDelete

            permissionsContainer.addView(permView)

            val permCheckboxes = PermissionCheckboxes(
                checkCanView, checkCanCreate, checkCanEdit, checkCanDelete, checkSelectAll
            )

            // Handle "All" checkbox - when checked, check all others
            checkSelectAll.setOnCheckedChangeListener { _, isChecked ->
                checkCanView.isChecked = isChecked
                checkCanCreate.isChecked = isChecked
                checkCanEdit.isChecked = isChecked
                checkCanDelete.isChecked = isChecked
            }

            // Update "All" checkbox state when individual checkboxes change
            val updateAllCheckbox = {
                val allChecked = checkCanView.isChecked && checkCanCreate.isChecked &&
                                checkCanEdit.isChecked && checkCanDelete.isChecked
                checkSelectAll.setOnCheckedChangeListener(null)
                checkSelectAll.isChecked = allChecked
                checkSelectAll.setOnCheckedChangeListener { _, isChecked ->
                    checkCanView.isChecked = isChecked
                    checkCanCreate.isChecked = isChecked
                    checkCanEdit.isChecked = isChecked
                    checkCanDelete.isChecked = isChecked
                }
            }

            checkCanView.setOnCheckedChangeListener { _, _ -> updateAllCheckbox() }
            checkCanCreate.setOnCheckedChangeListener { _, _ -> updateAllCheckbox() }
            checkCanEdit.setOnCheckedChangeListener { _, _ -> updateAllCheckbox() }
            checkCanDelete.setOnCheckedChangeListener { _, _ -> updateAllCheckbox() }

            permissionViews[moduleKey.name] = permCheckboxes
        }
    }

    private fun updatePermissionsVisibility(isAdmin: Boolean) {
        val strings = LocalizationManager.strings
        tvAdminNote.text = strings.admin // "Administrators have all permissions"

        if (isAdmin) {
            tvAdminNote.visibility = View.VISIBLE
            permissionsContainer.alpha = 0.5f
            // Disable all permission checkboxes
            permissionViews.values.forEach { it.setEnabled(false) }
        } else {
            tvAdminNote.visibility = View.GONE
            permissionsContainer.alpha = 1.0f
            // Enable all permission checkboxes
            permissionViews.values.forEach { it.setEnabled(true) }
        }
    }

    private fun loadUser() {
        val strings = LocalizationManager.strings

        lifecycleScope.launch {
            try {
                val (user, permissions) = withContext(Dispatchers.IO) {
                    val u = userId?.let { sdk.userRepository.getById(it) }
                    val p = if (u != null) sdk.userPermissionRepository.getPermissionsForUser(u.id) else emptyList()
                    Pair(u, p)
                }

                if (user != null) {
                    currentUserIsActive = user.isActive

                    editFullName.setText(user.fullName)
                    editUsername.setText(user.username)
                    editPassword.hint = strings.password // Leave blank to keep unchanged
                    checkIsAdmin.isChecked = user.isAdmin
                    checkIsActive.isChecked = user.isActive

                    updateToggleButtonText()

                    // Load permissions
                    permissions.forEach { permission ->
                        permissionViews[permission.module]?.apply {
                            canView.isChecked = permission.canView
                            canCreate.isChecked = permission.canCreate
                            canEdit.isChecked = permission.canEdit
                            canDelete.isChecked = permission.canDelete
                        }
                    }

                    updatePermissionsVisibility(user.isAdmin)
                }
            } catch (e: Exception) {
                Toast.makeText(this@UserAddEditActivity, "${strings.error}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateToggleButtonText() {
        val strings = LocalizationManager.strings
        if (currentUserIsActive) {
            btnToggleActive.text = strings.deactivate
            btnToggleActive.setBackgroundColor(getColor(android.R.color.holo_orange_dark))
        } else {
            btnToggleActive.text = strings.reactivate
            btnToggleActive.setBackgroundColor(getColor(android.R.color.holo_green_dark))
        }
    }

    private fun saveUser() {
        val strings = LocalizationManager.strings
        val fullName = editFullName.text.toString().trim()
        val username = editUsername.text.toString().trim()
        val password = editPassword.text.toString()
        val isAdmin = checkIsAdmin.isChecked
        val isActive = checkIsActive.isChecked

        if (fullName.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, strings.fieldRequired.replace("{field}", strings.fullName), Toast.LENGTH_SHORT).show()
            return
        }

        if (!isEditMode && password.isEmpty()) {
            Toast.makeText(this, strings.fieldRequired.replace("{field}", strings.password), Toast.LENGTH_SHORT).show()
            return
        }

        // Validate password complexity for new users or when password is provided in edit mode
        if (password.isNotEmpty()) {
            val validationResult = PasswordPolicy.validate(password)
            if (!validationResult.isValid) {
                val firstError = validationResult.errors.firstOrNull()
                if (firstError != null) {
                    Toast.makeText(this, PasswordPolicy.getErrorMessage(firstError, strings), Toast.LENGTH_SHORT).show()
                }
                return
            }
        }

        lifecycleScope.launch {
            try {
                val currentUser = authManager.getUsername()
                val timestamp = System.currentTimeMillis()

                withContext(Dispatchers.IO) {
                    if (isEditMode) {
                        // Update existing user
                        val existingUser = userId?.let { sdk.userRepository.getById(it) }
                        if (existingUser != null) {
                            val updatedUser = existingUser.copy(
                                fullName = fullName,
                                username = username,
                                password = if (password.isNotEmpty()) PasswordHasher.hashPassword(password) else existingUser.password,
                                isAdmin = isAdmin,
                                isActive = isActive,
                                updatedAt = timestamp,
                                updatedBy = currentUser
                            )
                            sdk.userRepository.update(updatedUser)

                            // Update permissions
                            savePermissions(existingUser.id, currentUser, timestamp)

                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@UserAddEditActivity, strings.success, Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        // Check if username already exists
                        val existingUser = sdk.userRepository.getByUsername(username)
                        if (existingUser != null) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@UserAddEditActivity, strings.usernameAlreadyExists, Toast.LENGTH_SHORT).show()
                            }
                            return@withContext
                        }

                        // Create new user
                        val newUserId = UUID.randomUUID().toString()
                        val newUser = User(
                            id = newUserId,
                            fullName = fullName,
                            username = username,
                            password = PasswordHasher.hashPassword(password),
                            isAdmin = isAdmin,
                            isActive = isActive,
                            createdAt = timestamp,
                            updatedAt = timestamp,
                            createdBy = currentUser,
                            updatedBy = currentUser
                        )
                        sdk.userRepository.insert(newUser)

                        // Save permissions
                        savePermissions(newUserId, currentUser, timestamp)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@UserAddEditActivity, strings.success, Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                finish()
            } catch (e: Exception) {
                Toast.makeText(this@UserAddEditActivity, "${strings.error}: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun savePermissions(userId: String, createdBy: String, timestamp: Long) {
        // Delete existing permissions
        sdk.userPermissionRepository.deletePermissionsForUser(userId)

        // Insert new permissions
        permissionViews.forEach { (moduleKey, checkboxes) ->
            val permission = UserPermission(
                id = UUID.randomUUID().toString(),
                userId = userId,
                module = moduleKey,
                canView = checkboxes.canView.isChecked,
                canCreate = checkboxes.canCreate.isChecked,
                canEdit = checkboxes.canEdit.isChecked,
                canDelete = checkboxes.canDelete.isChecked,
                createdAt = timestamp,
                updatedAt = timestamp,
                createdBy = createdBy,
                updatedBy = createdBy
            )
            sdk.userPermissionRepository.insert(permission)
        }
    }

    private fun toggleUserActiveStatus() {
        val strings = LocalizationManager.strings
        val actionText = if (currentUserIsActive) strings.deactivate else strings.reactivate

        AlertDialog.Builder(this)
            .setTitle(strings.confirm)
            .setMessage("${strings.confirm}?")
            .setPositiveButton(actionText) { _, _ ->
                lifecycleScope.launch {
                    try {
                        val (user, shouldContinue) = withContext(Dispatchers.IO) {
                            val u = userId?.let { sdk.userRepository.getById(it) }
                            if (u == null) {
                                return@withContext Pair(null, false)
                            }

                            // Check if trying to deactivate the last active admin
                            if (currentUserIsActive && u.isAdmin) {
                                val adminCount = sdk.userRepository.countActiveAdmins()
                                if (adminCount <= 1) {
                                    return@withContext Pair(u, false)
                                }
                            }
                            Pair(u, true)
                        }

                        if (!shouldContinue) {
                            Toast.makeText(
                                this@UserAddEditActivity,
                                strings.cannotDelete,
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }

                        if (user != null) {
                            val newStatus = !currentUserIsActive
                            val updatedUser = user.copy(
                                isActive = newStatus,
                                updatedAt = System.currentTimeMillis(),
                                updatedBy = authManager.getUsername()
                            )

                            withContext(Dispatchers.IO) {
                                sdk.userRepository.update(updatedUser)
                            }

                            currentUserIsActive = newStatus
                            checkIsActive.isChecked = newStatus
                            updateToggleButtonText()

                            val message = if (newStatus) strings.reactivate else strings.deactivate
                            Toast.makeText(this@UserAddEditActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@UserAddEditActivity, "${strings.error}: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton(strings.cancel, null)
            .show()
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

    private data class PermissionCheckboxes(
        val canView: CheckBox,
        val canCreate: CheckBox,
        val canEdit: CheckBox,
        val canDelete: CheckBox,
        val selectAllCheckbox: CheckBox? = null
    ) {
        fun setEnabled(enabled: Boolean) {
            canView.isEnabled = enabled
            canCreate.isEnabled = enabled
            canEdit.isEnabled = enabled
            canDelete.isEnabled = enabled
            selectAllCheckbox?.isEnabled = enabled
        }
    }
}
