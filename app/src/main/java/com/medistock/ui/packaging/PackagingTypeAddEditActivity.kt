package com.medistock.ui.packaging

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.medistock.R
import com.medistock.data.entities.PackagingType
import com.medistock.ui.viewmodel.PackagingTypeViewModel
import com.medistock.util.AuthManager

class PackagingTypeAddEditActivity : AppCompatActivity() {

    private lateinit var viewModel: PackagingTypeViewModel
    private lateinit var authManager: AuthManager
    private var packagingTypeId: String? = null
    private var existingPackagingType: PackagingType? = null

    private lateinit var editName: EditText
    private lateinit var editLevel1Name: EditText
    private lateinit var checkboxHasTwoLevels: CheckBox
    private lateinit var layoutLevel2: LinearLayout
    private lateinit var editLevel2Name: EditText
    private lateinit var editConversionFactor: EditText
    private lateinit var checkboxActive: CheckBox
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_packaging_type_add_edit)

        packagingTypeId = intent.getStringExtra("PACKAGING_TYPE_ID")?.takeIf { it.isNotBlank() }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (packagingTypeId == null) {
            "Add Packaging Type"
        } else {
            "Edit Packaging Type"
        }

        viewModel = ViewModelProvider(this)[PackagingTypeViewModel::class.java]
        authManager = AuthManager.getInstance(this)

        initViews()
        setupListeners()
        loadData()
    }

    private fun initViews() {
        editName = findViewById(R.id.editPackagingTypeName)
        editLevel1Name = findViewById(R.id.editLevel1Name)
        checkboxHasTwoLevels = findViewById(R.id.checkboxHasTwoLevels)
        layoutLevel2 = findViewById(R.id.layoutLevel2)
        editLevel2Name = findViewById(R.id.editLevel2Name)
        editConversionFactor = findViewById(R.id.editConversionFactor)
        checkboxActive = findViewById(R.id.checkboxActive)
        btnSave = findViewById(R.id.btnSavePackagingType)
    }

    private fun setupListeners() {
        checkboxHasTwoLevels.setOnCheckedChangeListener { _, isChecked ->
            layoutLevel2.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                editLevel2Name.text.clear()
                editConversionFactor.text.clear()
            }
        }

        btnSave.setOnClickListener {
            savePackagingType()
        }
    }

    private fun loadData() {
        packagingTypeId?.let { id ->
            viewModel.getById(id) { packagingType ->
                runOnUiThread {
                    packagingType?.let {
                        existingPackagingType = it
                        populateFields(it)
                    }
                }
            }
        }
    }

    private fun populateFields(packagingType: PackagingType) {
        editName.setText(packagingType.name)
        editLevel1Name.setText(packagingType.level1Name)

        val hasTwoLevels = packagingType.hasTwoLevels()
        checkboxHasTwoLevels.isChecked = hasTwoLevels

        if (hasTwoLevels) {
            layoutLevel2.visibility = View.VISIBLE
            editLevel2Name.setText(packagingType.level2Name ?: "")
            editConversionFactor.setText(packagingType.defaultConversionFactor?.toString() ?: "")
        }

        checkboxActive.isChecked = packagingType.isActive
    }

    private fun savePackagingType() {
        val name = editName.text.toString().trim()
        val level1Name = editLevel1Name.text.toString().trim()
        val hasTwoLevels = checkboxHasTwoLevels.isChecked

        // Validation
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a name", Toast.LENGTH_SHORT).show()
            return
        }

        if (level1Name.isEmpty()) {
            Toast.makeText(this, "Please enter level 1 name", Toast.LENGTH_SHORT).show()
            return
        }

        val level2Name: String?
        val conversionFactor: Double?

        if (hasTwoLevels) {
            level2Name = editLevel2Name.text.toString().trim()
            if (level2Name.isEmpty()) {
                Toast.makeText(this, "Please enter level 2 name", Toast.LENGTH_SHORT).show()
                return
            }

            val conversionText = editConversionFactor.text.toString().trim()
            if (conversionText.isEmpty()) {
                Toast.makeText(this, "Please enter conversion factor", Toast.LENGTH_SHORT).show()
                return
            }

            conversionFactor = conversionText.toDoubleOrNull()
            if (conversionFactor == null || conversionFactor <= 0) {
                Toast.makeText(this, "Conversion factor must be a positive number", Toast.LENGTH_SHORT).show()
                return
            }
        } else {
            level2Name = null
            conversionFactor = null
        }

        val isActive = checkboxActive.isChecked
        val currentUser = authManager.getUsername().ifBlank { "system" }
        val createdAt = existingPackagingType?.createdAt ?: System.currentTimeMillis()
        val createdBy = existingPackagingType?.createdBy?.ifBlank { currentUser } ?: currentUser

        val packagingType = if (packagingTypeId == null) {
            PackagingType(
                name = name,
                level1Name = level1Name,
                level2Name = level2Name,
                defaultConversionFactor = conversionFactor,
                isActive = isActive,
                displayOrder = existingPackagingType?.displayOrder ?: 0,
                createdAt = createdAt,
                updatedAt = System.currentTimeMillis(),
                createdBy = createdBy,
                updatedBy = currentUser
            )
        } else {
            PackagingType(
                id = packagingTypeId!!,
                name = name,
                level1Name = level1Name,
                level2Name = level2Name,
                defaultConversionFactor = conversionFactor,
                isActive = isActive,
                displayOrder = existingPackagingType?.displayOrder ?: 0,
                createdAt = createdAt,
                updatedAt = System.currentTimeMillis(),
                createdBy = createdBy,
                updatedBy = currentUser
            )
        }

        val callback = {
            runOnUiThread {
                Toast.makeText(
                    this@PackagingTypeAddEditActivity,
                    "Packaging type saved",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }

        if (packagingTypeId == null) {
            viewModel.insert(packagingType) { _ -> callback() }
        } else {
            viewModel.update(packagingType, callback)
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
