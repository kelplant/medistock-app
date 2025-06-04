package com.medistock.ui.product

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.Category
import com.medistock.data.entities.Product
import com.medistock.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductAddActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var editName: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var spinnerUnit: Spinner
    private lateinit var spinnerMarginType: Spinner
    private lateinit var editMarginValue: EditText
    private lateinit var textFinalPrice: TextView
    private lateinit var btnSave: Button

    private var categories: List<Category> = emptyList()
    private var selectedCategoryId: Long = 0L
    private var selectedUnit: String = ""
    private var selectedMarginType: String = "fixed"
    private var enteredMarginValue: Double = 0.0
    private var currentSiteId: Long = 0L
    private var enteredPurchasePrice: Double = 0.0  // Si tu veux gérer prix d’achat

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_add)

        db = AppDatabase.getInstance(this)

        editName = findViewById(R.id.editProductName)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        spinnerUnit = findViewById(R.id.spinnerUnit)
        spinnerMarginType = findViewById(R.id.spinnerMarginType)
        editMarginValue = findViewById(R.id.editMarginValue)
        textFinalPrice = findViewById(R.id.textFinalPrice)
        btnSave = findViewById(R.id.btnSaveProduct)

        // Charger categories depuis DB
        lifecycleScope.launch {
            categories = db.categoryDao().getAll()
            val categoryNames = categories.map { it.name }
            val categoryAdapter = ArrayAdapter(this@ProductAddActivity, android.R.layout.simple_spinner_item, categoryNames)
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerCategory.adapter = categoryAdapter
        }

        // Unités exemple fixe (tu peux adapter)
        val units = listOf("kg", "liter", "piece")
        val unitAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, units)
        unitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUnit.adapter = unitAdapter

        // Types de marge fixe ou pourcentage
        val marginTypes = listOf("fixed", "percentage")
        val marginTypeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, marginTypes)
        marginTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMarginType.adapter = marginTypeAdapter

        currentSiteId = PrefsHelper.getActiveSiteId(this)

        // Ecoute changements marge et nom produit pour recalcul prix final
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = calculateFinalPrice()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
        }

        editMarginValue.addTextChangedListener(textWatcher)

        spinnerMarginType.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedMarginType = marginTypes[position]
                calculateFinalPrice()
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
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        btnSave.setOnClickListener {
            val productName = editName.text.toString().trim()
            val marginText = editMarginValue.text.toString()
            enteredMarginValue = marginText.toDoubleOrNull() ?: 0.0

            if (productName.isEmpty()) {
                Toast.makeText(this, getString(R.string.error_enter_product_name), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (selectedCategoryId == 0L) {
                Toast.makeText(this, getString(R.string.error_select_category), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Ici on suppose que purchasePrice = 0 car non géré dans UI (tu peux l'ajouter)
            val purchasePrice = 0.0

            // Création produit
            val product = Product(
                name = productName,
                categoryId = selectedCategoryId,
                unit = selectedUnit,
                marginType = selectedMarginType,
                marginValue = enteredMarginValue,
                unitVolume = 1.0, // valeur fixe ou ajouter input si nécessaire
                siteId = currentSiteId
            )

            lifecycleScope.launch {
                db.productDao().insert(product)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProductAddActivity, getString(R.string.product_added), Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun calculateFinalPrice() {
        val marginValue = editMarginValue.text.toString().toDoubleOrNull() ?: 0.0
        val basePrice = 100.0 // valeur fixe ici, ou tu peux demander un prix d'achat pour calcul
        val finalPrice = when (selectedMarginType) {
            "fixed" -> basePrice + marginValue
            "percentage" -> basePrice * (1 + marginValue / 100)
            else -> basePrice
        }
        textFinalPrice.text = getString(R.string.final_price_template, finalPrice)
    }
}
