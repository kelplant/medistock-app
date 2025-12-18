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
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductAddEditActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var editName: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerUnit: Spinner
    private lateinit var spinnerMarginType: Spinner
    private lateinit var editMarginValue: EditText
    private lateinit var editUnitVolume: EditText
    private lateinit var textUnitVolumeLabel: TextView
    private lateinit var textMarginInfo: TextView
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button

    private var categories: List<Category> = emptyList()
    private var selectedCategoryId: Long = 0L
    private var selectedUnit: String = "Bottle"
    private var selectedMarginType: String = "percentage"
    private var enteredMarginValue: Double = 0.0
    private var enteredUnitVolume: Double = 0.0
    private var currentSiteId: Long = 0L
    private var productId: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_add_edit)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        db = AppDatabase.getInstance(this)

        editName = findViewById(R.id.editProductName)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerUnit = findViewById(R.id.spinnerUnit)
        spinnerMarginType = findViewById(R.id.spinnerMarginType)
        editMarginValue = findViewById(R.id.editMarginValue)
        editUnitVolume = findViewById(R.id.editUnitVolume)
        textUnitVolumeLabel = findViewById(R.id.textUnitVolumeLabel)
        textMarginInfo = findViewById(R.id.textMarginInfo)
        btnSave = findViewById(R.id.btnSaveProduct)
        btnDelete = findViewById(R.id.btnDeleteProduct)

        currentSiteId = PrefsHelper.getActiveSiteId(this)

        // Package types: Bottle (ml) or Box (tablets)
        val units = listOf("Bottle", "Box")
        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUnit.adapter = unitAdapter

        // Types de marge fixe ou pourcentage
        val marginTypes = listOf("percentage", "fixed")
        val marginTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, marginTypes)
        marginTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMarginType.adapter = marginTypeAdapter

        // Charger categories depuis DB
        lifecycleScope.launch {
            categories = db.categoryDao().getAll().first()
            if (categories.isEmpty()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        this@ProductAddEditActivity,
                        "No categories available. Please create a category first.",
                        Toast.LENGTH_LONG
                    ).show()
                    btnSave.isEnabled = false
                }
            } else {
                val categoryNames = categories.map { it.name }
                val categoryAdapter = ArrayAdapter(this@ProductAddEditActivity, android.R.layout.simple_spinner_item, categoryNames)
                categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerCategory.adapter = categoryAdapter

                // Si mode édition, charger le produit
                productId = intent.getLongExtra("PRODUCT_ID", -1).takeIf { it != -1L }
                if (productId != null) {
                    supportActionBar?.title = "Edit Product"
                    btnDelete.visibility = View.VISIBLE
                    loadProduct(productId!!)
                } else {
                    supportActionBar?.title = "Add Product"
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

        spinnerMarginType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedMarginType = marginTypes[position]
                updateMarginInfo()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedCategoryId = categories.getOrNull(position)?.id ?: 0L
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
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnSave.setOnClickListener {
            saveProduct()
        }

        btnDelete.setOnClickListener {
            confirmDelete()
        }
    }

    private fun loadProduct(id: Long) {
        lifecycleScope.launch {
            val product = db.productDao().getById(id).first()
            withContext(Dispatchers.Main) {
                if (product != null) {
                    editName.setText(product.name)
                    editMarginValue.setText((product.marginValue ?: 0.0).toString())
                    editUnitVolume.setText((product.unitVolume ?: 0.0).toString())

                    // Sélectionner la catégorie
                    val categoryIndex = categories.indexOfFirst { it.id == product.categoryId }
                    if (categoryIndex >= 0) {
                        spinnerCategory.setSelection(categoryIndex)
                    }

                    // Select unit
                    val unitIndex = listOf("Bottle", "Box").indexOf(product.unit)
                    if (unitIndex >= 0) {
                        spinnerUnit.setSelection(unitIndex)
                    }

                    // Sélectionner le type de marge
                    val marginTypeIndex = listOf("percentage", "fixed").indexOf(product.marginType)
                    if (marginTypeIndex >= 0) {
                        spinnerMarginType.setSelection(marginTypeIndex)
                    }

                    selectedCategoryId = product.categoryId ?: 0L
                    selectedUnit = product.unit
                    selectedMarginType = product.marginType ?: "percentage"
                }
            }
        }
    }

    private fun saveProduct() {
        val productName = editName.text.toString().trim()
        val marginText = editMarginValue.text.toString()
        val unitVolumeText = editUnitVolume.text.toString()

        enteredMarginValue = marginText.toDoubleOrNull() ?: 0.0
        enteredUnitVolume = unitVolumeText.toDoubleOrNull() ?: 0.0

        if (productName.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_enter_product_name), Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedCategoryId == 0L) {
            Toast.makeText(this, getString(R.string.error_select_category), Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedUnit.isEmpty()) {
            Toast.makeText(this, "Select a packaging type", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentSiteId == 0L) {
            Toast.makeText(this, "No active site. Please select a site first.", Toast.LENGTH_LONG).show()
            return
        }

        if (enteredMarginValue <= 0.0) {
            Toast.makeText(this, "Enter a valid margin", Toast.LENGTH_SHORT).show()
            return
        }

        if (enteredUnitVolume <= 0.0) {
            val volumeLabel = if (selectedUnit == "Bottle") "volume in ml" else "number of tablets"
            Toast.makeText(this, "Enter $volumeLabel", Toast.LENGTH_SHORT).show()
            return
        }

        // Création/Modification produit
        val product = Product(
            id = productId ?: 0L,
            name = productName,
            categoryId = selectedCategoryId,
            unit = selectedUnit,
            marginType = selectedMarginType,
            marginValue = enteredMarginValue,
            unitVolume = enteredUnitVolume,
            siteId = currentSiteId
        )

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
