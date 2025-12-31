package com.medistock.ui.product

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.Category
import com.medistock.data.entities.Product
import com.medistock.util.AuthManager
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductAddActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var authManager: AuthManager
    private lateinit var editName: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerUnit: Spinner
    private lateinit var spinnerMarginType: Spinner
    private lateinit var editMarginValue: EditText
    private lateinit var editDescription: EditText
    private lateinit var editUnitVolume: EditText
    private lateinit var textUnitVolumeLabel: TextView
    private lateinit var textMarginInfo: TextView
    private lateinit var btnSave: Button

    private var categories: List<Category> = emptyList()
    private var selectedCategoryId: String? = null
    private var selectedUnit: String = "Bottle"
    private var selectedMarginType: String = "percentage"
    private var enteredMarginValue: Double = 0.0
    private var enteredUnitVolume: Double = 0.0
    private var currentSiteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_add)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = AppDatabase.getInstance(this)
        authManager = AuthManager.getInstance(this)

        editName = findViewById(R.id.editProductName)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerUnit = findViewById(R.id.spinnerUnit)
        spinnerMarginType = findViewById(R.id.spinnerMarginType)
        editMarginValue = findViewById(R.id.editMarginValue)
        editDescription = findViewById(R.id.editProductDescription)
        editUnitVolume = findViewById(R.id.editUnitVolume)
        textUnitVolumeLabel = findViewById(R.id.textUnitVolumeLabel)
        textMarginInfo = findViewById(R.id.textMarginInfo)
        btnSave = findViewById(R.id.btnSaveProduct)

        // Load categories from DB
        lifecycleScope.launch {
            categories = db.categoryDao().getAll().first()
            if (categories.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProductAddActivity,
                        "No categories available. Please create a category first.",
                        Toast.LENGTH_LONG
                    ).show()
                    btnSave.isEnabled = false
                }
            } else {
                val categoryNames = categories.map { it.name }
                val categoryAdapter = ArrayAdapter(this@ProductAddActivity, android.R.layout.simple_spinner_item, categoryNames)
                categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategory.adapter = categoryAdapter
            }
        }

        // Package types: Bottle (ml), Box, Tablet, ml, Units
        val units = listOf("Bottle", "Box", "Tablet", "ml", "Units")
        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUnit.adapter = unitAdapter

        // Fixed or percentage margin types
        val marginTypes = listOf("percentage", "fixed")
        val marginTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, marginTypes)
        marginTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMarginType.adapter = marginTypeAdapter

        currentSiteId = PrefsHelper.getActiveSiteId(this)

        // Update info text when margin changes
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = updateMarginInfo()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        }

        editMarginValue.addTextChangedListener(textWatcher)

        spinnerMarginType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedMarginType = marginTypes[position]
                updateMarginInfo()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedCategoryId = categories.getOrNull(position)?.id
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerUnit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedUnit = units.getOrNull(position) ?: ""
                // Adapt label based on package type
                when (selectedUnit) {
                    "Bottle" -> {
                        textUnitVolumeLabel.text = "Volume per bottle (ml)"
                        editUnitVolume.hint = "Ex: 100"
                    }
                    "Box" -> {
                        textUnitVolumeLabel.text = "Number of tablets per box"
                        editUnitVolume.hint = "Ex: 30"
                    }
                    "Tablet" -> {
                        textUnitVolumeLabel.text = "Quantity per unit"
                        editUnitVolume.hint = "Ex: 1"
                    }
                    "ml" -> {
                        textUnitVolumeLabel.text = "Volume per unit (ml)"
                        editUnitVolume.hint = "Ex: 1"
                    }
                    "Units" -> {
                        textUnitVolumeLabel.text = "Quantity per unit"
                        editUnitVolume.hint = "Ex: 1"
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnSave.setOnClickListener {
            val productName = editName.text.toString().trim()
            val descriptionText = editDescription.text.toString().trim().ifBlank { null }
            val marginText = editMarginValue.text.toString()
            val unitVolumeText = editUnitVolume.text.toString()

            enteredMarginValue = marginText.toDoubleOrNull() ?: 0.0
            enteredUnitVolume = unitVolumeText.toDoubleOrNull() ?: 0.0

            if (productName.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_enter_product_name), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedCategoryId == null) {
                Toast.makeText(this, getString(R.string.error_select_category), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedUnit.isEmpty()) {
                Toast.makeText(this, "Select a packaging type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentSiteId.isNullOrBlank()) {
                Toast.makeText(this, "No active site. Please select a site first.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (enteredMarginValue <= 0.0) {
                Toast.makeText(this, "Enter a valid margin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (enteredUnitVolume <= 0.0) {
                val volumeLabel = when (selectedUnit) {
                    "Bottle" -> "volume in ml"
                    "Box" -> "number of tablets"
                    "Tablet" -> "quantity"
                    "ml" -> "volume in ml"
                    "Units" -> "quantity"
                    else -> "quantity"
                }
                Toast.makeText(this, "Enter $volumeLabel", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create product
            val currentUser = authManager.getUsername().ifBlank { "system" }
            val product = Product(
                name = productName,
                categoryId = selectedCategoryId,
                unit = selectedUnit,
                marginType = selectedMarginType,
                marginValue = enteredMarginValue,
                description = descriptionText,
                unitVolume = enteredUnitVolume,
                siteId = currentSiteId!!,
                createdBy = currentUser,
                updatedBy = currentUser
            )

            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        db.productDao().insert(product)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ProductAddActivity, getString(R.string.product_added), Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ProductAddActivity,
                            "Error adding product: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    private fun updateMarginInfo() {
        val marginValue = editMarginValue.text.toString().toDoubleOrNull() ?: 0.0
        val marginInfo = when (selectedMarginType) {
            "fixed" -> "Selling price will be: Purchase price + $marginValue"
            "percentage" -> "Selling price will be: Purchase price × (1 + ${marginValue}%)"
            else -> "Margin configuration"
        }
        textMarginInfo.text = marginInfo
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
