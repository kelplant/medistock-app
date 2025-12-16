package com.medistock.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.data.entities.CurrentStock

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

        // Display quantity on hand
        holder.quantityText?.text = "Stock: ${stock.quantityOnHand} ${stock.unit}"

        // Color coding for stock levels
        when {
            stock.quantityOnHand <= 0 -> holder.quantityText?.setTextColor(Color.RED)
            stock.quantityOnHand < 10 -> holder.quantityText?.setTextColor(Color.rgb(255, 140, 0)) // Orange
            else -> holder.quantityText?.setTextColor(Color.rgb(0, 128, 0)) // Green
        }

        // Display category if available
        holder.categoryText?.text = stock.categoryName
    }

    override fun getItemCount() = stockItems.size

    fun updateData(newStockItems: List<CurrentStock>) {
        stockItems = newStockItems
        notifyDataSetChanged()
    }
}