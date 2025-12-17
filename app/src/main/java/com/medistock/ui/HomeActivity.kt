package com.medistock.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.medistock.R
import com.medistock.ui.sales.SaleListActivity
import com.medistock.ui.manage.ManageProductMenuActivity
import com.medistock.ui.stock.StockListActivity
import com.medistock.ui.movement.StockMovementListActivity
import com.medistock.ui.purchase.PurchaseActivity
import com.medistock.ui.inventory.InventoryActivity
import com.medistock.ui.admin.AdminActivity

class HomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        findViewById<android.view.View>(R.id.viewStockButton).setOnClickListener {
            startActivity(Intent(this, StockListActivity::class.java))
        }

        findViewById<android.view.View>(R.id.sellProductButton).setOnClickListener {
            startActivity(Intent(this, SaleListActivity::class.java))
        }

        findViewById<android.view.View>(R.id.manageProductButton).setOnClickListener {
            startActivity(Intent(this, ManageProductMenuActivity::class.java))
        }

        findViewById<android.view.View>(R.id.stockMovementButton).setOnClickListener {
            startActivity(Intent(this, StockMovementListActivity::class.java))
        }

        findViewById<android.view.View>(R.id.purchaseButton).setOnClickListener {
            startActivity(Intent(this, PurchaseActivity::class.java))
        }

        findViewById<android.view.View>(R.id.inventoryButton).setOnClickListener {
            startActivity(Intent(this, InventoryActivity::class.java))
        }

        findViewById<android.view.View>(R.id.adminButton).setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }
    }
}