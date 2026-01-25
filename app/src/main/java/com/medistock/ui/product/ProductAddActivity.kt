package com.medistock.ui.product

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.MedistockSDK
import com.medistock.shared.domain.model.Category
import com.medistock.shared.domain.model.PackagingType
import com.medistock.shared.domain.model.Product
import com.medistock.util.AuthManager
import com.medistock.util.PrefsHelper
import com.medistock.shared.i18n.L
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class ProductAddActivity : AppCompatActivity() {

    private lateinit var sdk: MedistockSDK
    private lateinit var authManager: AuthManager
    private lateinit var editName: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerPackagingType: Spinner
    private lateinit var spinnerMarginType: Spinner
    private lateinit var editMarginValue: EditText
    private lateinit var editDescription: EditText
    private lateinit var editUnitVolume: EditText
    private lateinit var textUnitVolumeLabel: TextView
    private lateinit var textMarginInfo: TextView
    private lateinit var btnSave: Button

    private var categories: List<Category> = emptyList()
    private var packagingTypes: List<PackagingType> = emptyList()
    private var selectedCategoryId: String? = null
    private var selectedPackagingTypeId: String? = null
    private var selectedMarginType: String = "percentage"
    private var enteredMarginValue: Double = 0.0
    private var enteredUnitVolume: Double = 0.0
    private var currentSiteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_add)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        sdk = MedistockApplication.sdk
        authManager = AuthManager.getInstance(this)

        editName = findViewById(R.id.editProductName)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerPackagingType = findViewById(R.id.spinnerUnit)
        spinnerMarginType = findViewById(R.id.spinnerMarginType)
        editMarginValue = findViewById(R.id.editMarginValue)
        editDescription = findViewById(R.id.editProductDescription)
        editUnitVolume = findViewById(R.id.editUnitVolume)
        textUnitVolumeLabel = findViewById(R.id.textUnitVolumeLabel)
        textMarginInfo = findViewById(R.id.textMarginInfo)
        btnSave = findViewById(R.id.btnSaveProduct)

        // Load categories and packaging types from DB
        lifecycleScope.launch {
            categories = withContext(Dispatchers.IO) {
                sdk.categoryRepository.getAll()
            }
            packagingTypes = withContext(Dispatchers.IO) {
                sdk.packagingTypeRepository.getAll()
            }

            if (categories.isEmpty()) {
                Toast.makeText(
                    this@ProductAddActivity,
                    L.strings.noCategories,
                    Toast.LENGTH_LONG
                ).show()
                btnSave.isEnabled = false
            } else {
                val categoryNames = categories.map { it.name }
                val categoryAdapter = ArrayAdapter(this@ProductAddActivity, android.R.layout.simple_spinner_item, categoryNames)
                categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategory.adapter = categoryAdapter
            }

            if (packagingTypes.isEmpty()) {
                Toast.makeText(
                    this@ProductAddActivity,
                    L.strings.packagingType,
                    Toast.LENGTH_LONG
                ).show()
                btnSave.isEnabled = false
            } else {
                val packagingNames = packagingTypes.map { it.name }
                val packagingAdapter = ArrayAdapter(this@ProductAddActivity, android.R.layout.simple_spinner_item, packagingNames)
                packagingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerPackagingType.adapter = packagingAdapter
            }
        }

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

        spinnerPackagingType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                val packagingType = packagingTypes.getOrNull(position)
                selectedPackagingTypeId = packagingType?.id
                // Adapt label based on packaging type
                val unitName = packagingType?.level1Name ?: "unit"
                textUnitVolumeLabel.text = "${L.strings.quantity} / $unitName"
                editUnitVolume.hint = packagingType?.defaultConversionFactor?.toString() ?: "1"
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnSave.setOnClickListener {
            val productName = editName.text.toString().trim()
            val descriptionText = editDescription.text.toString().trim().ifBlank { null }
            val marginText = editMarginValue.text.toString()
            val unitVolumeText = editUnitVolume.text.toString()

            enteredMarginValue = marginText.toDoubleOrNull() ?: 0.0
            enteredUnitVolume = unitVolumeText.toDoubleOrNull() ?: 1.0

            if (productName.isEmpty()) {
                Toast.makeText(this, L.strings.required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedCategoryId == null) {
                Toast.makeText(this, L.strings.selectCategory, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedPackagingTypeId == null) {
                Toast.makeText(this, L.strings.packagingType, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentSiteId.isNullOrBlank()) {
                Toast.makeText(this, L.strings.selectSite, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (enteredMarginValue <= 0.0) {
                Toast.makeText(this, L.strings.valueMustBePositive, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (enteredUnitVolume <= 0.0) {
                Toast.makeText(this, L.strings.valueMustBePositive, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create product
            val currentUser = authManager.getUsername().ifBlank { "system" }
            val currentTime = System.currentTimeMillis()
            val product = Product(
                id = UUID.randomUUID().toString(),
                name = productName,
                categoryId = selectedCategoryId,
                packagingTypeId = selectedPackagingTypeId!!,
                selectedLevel = 1,
                unitVolume = enteredUnitVolume,
                marginType = selectedMarginType,
                marginValue = enteredMarginValue,
                description = descriptionText,
                siteId = currentSiteId!!,
                createdAt = currentTime,
                updatedAt = currentTime,
                createdBy = currentUser,
                updatedBy = currentUser
            )

            lifecycleScope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        sdk.productRepository.insert(product)
                    }
                    Toast.makeText(this@ProductAddActivity, L.strings.success, Toast.LENGTH_SHORT).show()
                    finish()
                } catch (e: Exception) {
                    Toast.makeText(
                        this@ProductAddActivity,
                        "${L.strings.error}: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun updateMarginInfo() {
        val marginValue = editMarginValue.text.toString().toDoubleOrNull() ?: 0.0
        val marginInfo = when (selectedMarginType) {
            "fixed" -> "${L.strings.sellingPrice}: ${L.strings.purchasePrice} + $marginValue"
            "percentage" -> "${L.strings.sellingPrice}: ${L.strings.purchasePrice} x (1 + ${marginValue}%)"
            else -> L.strings.margin
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
