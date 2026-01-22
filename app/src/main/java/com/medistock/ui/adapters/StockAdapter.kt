package com.medistock.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.shared.domain.model.CurrentStock

class StockAdapter(private var stockItems: List<CurrentStock>) :
    RecyclerView.Adapter<StockAdapter.StockViewHolder>() {

    inner class StockViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.textProductName)
        val unitText: TextView = view.findViewById(R.id.textProductUnit)
        val quantityText: TextView? = view.findViewById(R.id.textStockQuantity)
        val categoryText: TextView? = view.findViewById(R.id.textProductCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stock, parent, false)
        return StockViewHolder(view)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val stock = stockItems[position]
        holder.nameText.text = stock.productName
        holder.unitText.text = stock.unit

        // Display quantity on hand with alert indicators
        val alertIndicator = when {
            stock.quantityOnHand <= 0 -> "⚠️ "
            stock.minStock > 0 && stock.quantityOnHand <= stock.minStock -> "⚠️ "
            else -> ""
        }
        holder.quantityText?.text = "${alertIndicator}Stock: ${stock.quantityOnHand} ${stock.unit}"

        // Smart color coding based on thresholds
        val color = when {
            stock.quantityOnHand <= 0 -> Color.RED // Out of stock
            stock.minStock > 0 && stock.quantityOnHand <= stock.minStock -> Color.RED // Below minimum
            stock.minStock > 0 && stock.quantityOnHand <= stock.minStock * 1.5 -> Color.rgb(255, 140, 0) // Warning (orange)
            stock.maxStock > 0 && stock.quantityOnHand >= stock.maxStock -> Color.rgb(0, 100, 200) // Overstocked (blue)
            else -> Color.rgb(0, 128, 0) // Good level (green)
        }
        holder.quantityText?.setTextColor(color)

        // Display category if available
        holder.categoryText?.text = stock.categoryName
    }

    override fun getItemCount() = stockItems.size

    fun updateData(newStockItems: List<CurrentStock>) {
        stockItems = newStockItems
        notifyDataSetChanged()
    }
}