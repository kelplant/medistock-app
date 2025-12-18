package com.medistock.ui.user

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.User
import com.medistock.data.entities.UserPermission
import com.medistock.util.AuthManager
import com.medistock.util.Modules
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    private lateinit var db: AppDatabase
    private lateinit var authManager: AuthManager
    private var userId: Long = -1
    private var isEditMode = false
    private var currentUserIsActive = true

    private val permissionViews = mutableMapOf<String, PermissionCheckboxes>()

    private val modules = listOf(
        Modules.STOCK to "Stock",
        Modules.SALES to "Sales",
        Modules.PURCHASES to "Purchases",
        Modules.INVENTORY to "Inventory",
        Modules.PRODUCTS to "Products",
        Modules.SITES to "Sites",
        Modules.CATEGORIES to "Categories",
        Modules.ADMIN to "Administration"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = AuthManager.getInstance(this)

        // Check if user is admin
        if (!authManager.isAdmin()) {
            Toast.makeText(this, "Access denied: Administrators only", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_user_add_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = AppDatabase.getInstance(this)

        // Initialize views
        editFullName = findViewById(R.id.editFullName)
        editUsername = findViewById(R.id.editUsername)
        editPassword = findViewById(R.id.editPassword)
        checkIsAdmin = findViewById(R.id.checkIsAdmin)
        checkIsActive = findViewById(R.id.checkIsActive)
        btnSaveUser = findViewById(R.id.btnSaveUser)
        btnToggleActive = findViewById(R.id.btnDeleteUser) // Repurposed delete button
        tvAdminNote = findViewById(R.id.tvAdminNote)
        permissionsContainer = findViewById(R.id.permissionsContainer)

        // Setup permissions UI
        setupPermissionsUI()

        // Check admin checkbox behavior
        checkIsAdmin.setOnCheckedChangeListener { _, isChecked ->
            updatePermissionsVisibility(isChecked)
        }

        // Check if editing existing user
        userId = intent.getLongExtra("USER_ID", -1)
        if (userId != -1L) {
            isEditMode = true
            supportActionBar?.title = "Edit User"
            loadUser()
            btnToggleActive.visibility = View.VISIBLE
        } else {
            supportActionBar?.title = "New User"
        }

        btnSaveUser.setOnClickListener { saveUser() }
        btnToggleActive.setOnClickListener { toggleUserActiveStatus() }
    }

    private fun setupPermissionsUI() {
        modules.forEach { (moduleKey, moduleName) ->
            val permView = LayoutInflater.from(this).inflate(R.layout.item_permission, permissionsContainer, false)

            val tvModuleName = permView.findViewById<TextView>(R.id.tvModuleName)
            val checkSelectAll = permView.findViewById<CheckBox>(R.id.btnSelectAll)
            val checkCanView = permView.findViewById<CheckBox>(R.id.checkCanView)
            val checkCanCreate = permView.findViewById<CheckBox>(R.id.checkCanCreate)
            val checkCanEdit = permView.findViewById<CheckBox>(R.id.checkCanEdit)
            val checkCanDelete = permView.findViewById<CheckBox>(R.id.checkCanDelete)

            tvModuleName.text = moduleName
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

            permissionViews[moduleKey] = permCheckboxes
        }
    }

    private fun updatePermissionsVisibility(isAdmin: Boolean) {
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
        lifecycleScope.launch {
            try {
                val (user, permissions) = withContext(Dispatchers.IO) {
                    val u = db.userDao().getUserById(userId)
                    val p = if (u != null) db.userPermissionDao().getPermissionsForUser(userId) else emptyList()
                    Pair(u, p)
                }

                if (user != null) {
                    currentUserIsActive = user.isActive

                    editFullName.setText(user.fullName)
                    editUsername.setText(user.username)
                    editPassword.hint = "Leave blank to keep unchanged"
                    checkIsAdmin.isChecked = user.isAdmin
                    checkIsActive.isChecked = user.isActive

                    // Update toggle button text based on current status
                    runOnUiThread {
                        updateToggleButtonText()
                    }

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
                Toast.makeText(this@UserAddEditActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateToggleButtonText() {
        if (currentUserIsActive) {
            btnToggleActive.text = "Deactivate User"
            btnToggleActive.setBackgroundColor(getColor(android.R.color.holo_orange_dark))
        } else {
            btnToggleActive.text = "Reactivate User"
            btnToggleActive.setBackgroundColor(getColor(android.R.color.holo_green_dark))
        }
    }

    private fun saveUser() {
        val fullName = editFullName.text.toString().trim()
        val username = editUsername.text.toString().trim()
        val password = editPassword.text.toString()
        val isAdmin = checkIsAdmin.isChecked
        val isActive = checkIsActive.isChecked

        if (fullName.isEmpty() || username.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isEditMode && password.isEmpty()) {
            Toast.makeText(this, "Password is required for new users", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val currentUser = authManager.getUsername()
                val timestamp = System.currentTimeMillis()

                withContext(Dispatchers.IO) {
                    if (isEditMode) {
                        // Update existing user
                        val existingUser = db.userDao().getUserById(userId)
                        if (existingUser != null) {
                            val updatedUser = existingUser.copy(
                                fullName = fullName,
                                username = username,
                                password = if (password.isNotEmpty()) password else existingUser.password,
                                isAdmin = isAdmin,
                                isActive = isActive,
                                updatedAt = timestamp,
                                updatedBy = currentUser
                            )
                            db.userDao().updateUser(updatedUser)

                            // Update permissions
                            savePermissions(userId, currentUser, timestamp)

                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@UserAddEditActivity, "User updated", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        // Check if username already exists
                        val existingUser = db.userDao().getUserByUsername(username)
                        if (existingUser != null) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@UserAddEditActivity, "Username already exists", Toast.LENGTH_SHORT).show()
                            }
                            return@withContext
                        }

                        // Create new user
                        val newUser = User(
                            fullName = fullName,
                            username = username,
                            password = password,
                            isAdmin = isAdmin,
                            isActive = isActive,
                            createdAt = timestamp,
                            updatedAt = timestamp,
                            createdBy = currentUser,
                            updatedBy = currentUser
                        )
                        val newUserId = db.userDao().insertUser(newUser)

                        // Save permissions
                        savePermissions(newUserId, currentUser, timestamp)

                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@UserAddEditActivity, "User created", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                finish()
            } catch (e: Exception) {
                Toast.makeText(this@UserAddEditActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun savePermissions(userId: Long, createdBy: String, timestamp: Long) {
        // Delete existing permissions
        db.userPermissionDao().deleteAllPermissionsForUser(userId)

        // Insert new permissions
        val permissions = mutableListOf<UserPermission>()
        permissionViews.forEach { (moduleKey, checkboxes) ->
            val permission = UserPermission(
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
            permissions.add(permission)
        }
        db.userPermissionDao().insertPermissions(permissions)
    }

    private fun toggleUserActiveStatus() {
        val action = if (currentUserIsActive) "deactivate" else "reactivate"
        val actionCapitalized = if (currentUserIsActive) "Deactivate" else "Reactivate"

        AlertDialog.Builder(this)
            .setTitle("Confirm Action")
            .setMessage("Do you want to $action this user?")
            .setPositiveButton(actionCapitalized) { _, _ ->
                lifecycleScope.launch {
                    try {
                        val (user, shouldContinue) = withContext(Dispatchers.IO) {
                            val u = db.userDao().getUserById(userId)

                            // Check if trying to deactivate the last active admin
                            if (currentUserIsActive && u?.isAdmin == true) {
                                val adminCount = db.userDao().countActiveAdmins()
                                if (adminCount <= 1) {
                                    return@withContext Pair(u, false)
                                }
                            }
                            Pair(u, true)
                        }

                        if (!shouldContinue) {
                            Toast.makeText(
                                this@UserAddEditActivity,
                                "Cannot deactivate the last active administrator",
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
                                db.userDao().updateUser(updatedUser)
                            }

                            currentUserIsActive = newStatus
                            checkIsActive.isChecked = newStatus
                            updateToggleButtonText()

                            val message = if (newStatus) "User reactivated" else "User deactivated"
                            Toast.makeText(this@UserAddEditActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(this@UserAddEditActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
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
