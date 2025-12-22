package com.medistock.ui.product

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.Category
import com.medistock.data.entities.Product
import com.medistock.data.entities.PackagingType
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductAddEditActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var editName: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerPackagingType: Spinner
    private lateinit var spinnerSelectedLevel: Spinner
    private lateinit var spinnerMarginType: Spinner
    private lateinit var editMarginValue: EditText
    private lateinit var editConversionFactor: EditText
    private lateinit var textConversionFactorLabel: TextView
    private lateinit var textMarginInfo: TextView
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button

    private var categories: List<Category> = emptyList()
    private var packagingTypes: List<PackagingType> = emptyList()
    private var selectedCategoryId: String? = null
    private var selectedPackagingTypeId: String? = null
    private var selectedLevel: Int = 1
    private var selectedMarginType: String = "percentage"
    private var enteredMarginValue: Double = 0.0
    private var enteredConversionFactor: Double? = null
    private var currentSiteId: String? = null
    private var productId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_add_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = AppDatabase.getInstance(this)

        editName = findViewById(R.id.editProductName)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerPackagingType = findViewById(R.id.spinnerPackagingType)
        spinnerSelectedLevel = findViewById(R.id.spinnerSelectedLevel)
        spinnerMarginType = findViewById(R.id.spinnerMarginType)
        editMarginValue = findViewById(R.id.editMarginValue)
        editConversionFactor = findViewById(R.id.editConversionFactor)
        textConversionFactorLabel = findViewById(R.id.textConversionFactorLabel)
        textMarginInfo = findViewById(R.id.textMarginInfo)
        btnSave = findViewById(R.id.btnSaveProduct)
        btnDelete = findViewById(R.id.btnDeleteProduct)

        currentSiteId = PrefsHelper.getActiveSiteId(this)

        // Fixed or percentage margin types
        val marginTypes = listOf("percentage", "fixed")
        val marginTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, marginTypes)
        marginTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMarginType.adapter = marginTypeAdapter

        // Load categories and packaging types from DB
        lifecycleScope.launch {
            categories = db.categoryDao().getAll().first()
            packagingTypes = db.packagingTypeDao().getAllActive().first()

            if (categories.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProductAddEditActivity,
                        "No categories available. Please create a category first.",
                        Toast.LENGTH_LONG
                    ).show()
                    btnSave.isEnabled = false
                }
            } else if (packagingTypes.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProductAddEditActivity,
                        "No packaging types available. Please create packaging types first.",
                        Toast.LENGTH_LONG
                    ).show()
                    btnSave.isEnabled = false
                }
            } else {
                withContext(Dispatchers.Main) {
                    setupCategorySpinner()
                    setupPackagingTypeSpinner()
                    setupSpinnerListeners()

                    // If edit mode, load the product
                    productId = intent.getStringExtra("PRODUCT_ID")?.takeIf { it.isNotBlank() }
                    if (productId != null) {
                        supportActionBar?.title = "Edit Product"
                        btnDelete.visibility = View.VISIBLE
                        loadProduct(productId!!)
                    } else {
                        supportActionBar?.title = "Add Product"
                        // Set default level
                        updateLevelSpinner()
                    }
                }
            }
        }

        // Update info text when margin changes
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = updateMarginInfo()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        }

        editMarginValue.addTextChangedListener(textWatcher)

        btnSave.setOnClickListener {
            saveProduct()
        }

        btnDelete.setOnClickListener {
            confirmDelete()
        }
    }

    private fun setupCategorySpinner() {
        val categoryNames = categories.map { it.name }
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categoryNames)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = categoryAdapter
    }

    private fun setupPackagingTypeSpinner() {
        val packagingTypeNames = packagingTypes.map { it.name }
        val packagingTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, packagingTypeNames)
        packagingTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPackagingType.adapter = packagingTypeAdapter
    }

    private fun setupSpinnerListeners() {
        val marginTypes = listOf("percentage", "fixed")

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

        spinnerPackagingType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val packagingType = packagingTypes.getOrNull(position)
                selectedPackagingTypeId = packagingType?.id
                updateLevelSpinner()
                updateConversionFactorLabel()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerSelectedLevel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedLevel = position + 1 // Position 0 = Level 1, Position 1 = Level 2
                updateConversionFactorLabel()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun updateLevelSpinner() {
        val packagingType = packagingTypes.find { it.id == selectedPackagingTypeId }
        if (packagingType != null) {
            val levelOptions = if (packagingType.hasTwoLevels()) {
                listOf(
                    "Level 1: ${packagingType.level1Name}",
                    "Level 2: ${packagingType.level2Name}"
                )
            } else {
                listOf("${packagingType.level1Name}")
            }

            val levelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, levelOptions)
            levelAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerSelectedLevel.adapter = levelAdapter

            // Reset to level 1 by default
            selectedLevel = 1
            spinnerSelectedLevel.setSelection(0)
        }
    }

    private fun updateConversionFactorLabel() {
        val packagingType = packagingTypes.find { it.id == selectedPackagingTypeId }
        if (packagingType != null && packagingType.hasTwoLevels()) {
            textConversionFactorLabel.visibility = View.VISIBLE
            editConversionFactor.visibility = View.VISIBLE

            val level1 = packagingType.level1Name
            val level2 = packagingType.level2Name

            textConversionFactorLabel.text = "Conversion Factor (number of $level2 per $level1)"
            editConversionFactor.hint = "e.g.: ${packagingType.defaultConversionFactor ?: ""}"

            // Pre-fill with default if available
            if (editConversionFactor.text.isNullOrEmpty() && packagingType.defaultConversionFactor != null) {
                editConversionFactor.setText(packagingType.defaultConversionFactor.toString())
            }
        } else {
            textConversionFactorLabel.visibility = View.GONE
            editConversionFactor.visibility = View.GONE
            enteredConversionFactor = null
        }
    }

    private fun loadProduct(id: String) {
        lifecycleScope.launch {
            val product = db.productDao().getById(id).first()
            withContext(Dispatchers.Main) {
                if (product != null) {
                    editName.setText(product.name)
                    editMarginValue.setText((product.marginValue ?: 0.0).toString())

                    // Select the category
                    val categoryIndex = categories.indexOfFirst { it.id == product.categoryId }
                    if (categoryIndex >= 0) {
                        spinnerCategory.setSelection(categoryIndex)
                    }

                    // Select packaging type and level
                    if (product.packagingTypeId != null) {
                        val packagingTypeIndex = packagingTypes.indexOfFirst { it.id == product.packagingTypeId }
                        if (packagingTypeIndex >= 0) {
                            spinnerPackagingType.setSelection(packagingTypeIndex)
                            selectedPackagingTypeId = product.packagingTypeId
                            updateLevelSpinner()

                            // Select the level
                            if (product.selectedLevel != null) {
                                spinnerSelectedLevel.setSelection(product.selectedLevel - 1)
                                selectedLevel = product.selectedLevel
                            }
                        }

                        // Set conversion factor if available
                        if (product.conversionFactor != null) {
                            editConversionFactor.setText(product.conversionFactor.toString())
                        }
                    }

                    // Select the margin type
                    val marginTypeIndex = listOf("percentage", "fixed").indexOf(product.marginType)
                    if (marginTypeIndex >= 0) {
                        spinnerMarginType.setSelection(marginTypeIndex)
                    }

                    selectedCategoryId = product.categoryId
                    selectedMarginType = product.marginType ?: "percentage"
                }
            }
        }
    }

    private fun saveProduct() {
        val productName = editName.text.toString().trim()
        val marginText = editMarginValue.text.toString()
        val conversionFactorText = editConversionFactor.text.toString()

        enteredMarginValue = marginText.toDoubleOrNull() ?: 0.0
        enteredConversionFactor = conversionFactorText.toDoubleOrNull()

        if (productName.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_enter_product_name), Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedCategoryId == null) {
            Toast.makeText(this, getString(R.string.error_select_category), Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedPackagingTypeId == null) {
            Toast.makeText(this, "Select a packaging type", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentSiteId.isNullOrBlank()) {
            Toast.makeText(this, "No active site. Please select a site first.", Toast.LENGTH_LONG).show()
            return
        }

        if (enteredMarginValue <= 0.0) {
            Toast.makeText(this, "Enter a valid margin", Toast.LENGTH_SHORT).show()
            return
        }

        // Validate conversion factor for 2-level packaging types
        val packagingType = packagingTypes.find { it.id == selectedPackagingTypeId }
        if (packagingType != null && packagingType.hasTwoLevels()) {
            if (enteredConversionFactor == null || enteredConversionFactor!! <= 0.0) {
                Toast.makeText(this, "Enter a valid conversion factor", Toast.LENGTH_SHORT).show()
                return
            }
        }

        // Populate unit and unitVolume from selected PackagingType
        val effectiveUnit = packagingType?.getLevelName(selectedLevel) ?: "Units"
        val effectiveUnitVolume = enteredConversionFactor ?: packagingType?.defaultConversionFactor ?: 1.0

        // Create/Update product
        val product = if (productId == null) {
            Product(
                name = productName,
                categoryId = selectedCategoryId,
                packagingTypeId = selectedPackagingTypeId,
                selectedLevel = selectedLevel,
                conversionFactor = enteredConversionFactor,
                unit = effectiveUnit,
                unitVolume = effectiveUnitVolume,
                marginType = selectedMarginType,
                marginValue = enteredMarginValue,
                siteId = currentSiteId!!
            )
        } else {
            Product(
                id = productId!!,
                name = productName,
                categoryId = selectedCategoryId,
                packagingTypeId = selectedPackagingTypeId,
                selectedLevel = selectedLevel,
                conversionFactor = enteredConversionFactor,
                unit = effectiveUnit,
                unitVolume = effectiveUnitVolume,
                marginType = selectedMarginType,
                marginValue = enteredMarginValue,
                siteId = currentSiteId!!
            )
        }

        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (productId == null) {
                        db.productDao().insert(product)
                    } else {
                        db.productDao().update(product)
                    }
                }
                withContext(Dispatchers.Main) {
                    val message = if (productId == null) getString(R.string.product_added) else "Product updated successfully"
                    Toast.makeText(this@ProductAddEditActivity, message, Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProductAddEditActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle("Delete Product")
            .setMessage("Are you sure you want to delete this product?")
            .setPositiveButton("Delete") { _, _ ->
                deleteProduct()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteProduct() {
        if (productId == null) return

        lifecycleScope.launch {
            try {
                val product = db.productDao().getById(productId!!).first()
                if (product != null) {
                    withContext(Dispatchers.IO) {
                        db.productDao().delete(product)
                    }
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ProductAddEditActivity, "Product deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProductAddEditActivity,
                        "Error deleting: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
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
