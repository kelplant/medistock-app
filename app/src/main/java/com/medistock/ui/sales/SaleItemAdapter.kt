package com.medistock.ui.sales

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R

class SaleItemAdapter(
    private val items: MutableList<SaleItem>,
    private val onItemRemoved: (SaleItem) -> Unit
) : RecyclerView.Adapter<SaleItemAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textProductName: TextView = view.findViewById(R.id.textProductName)
        val textQuantityPrice: TextView = view.findViewById(R.id.textQuantityPrice)
        val textSubtotal: TextView = view.findViewById(R.id.textSubtotal)
        val btnRemoveItem: ImageButton = view.findViewById(R.id.btnRemoveItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sale_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.textProductName.text = item.productName
        holder.textQuantityPrice.text = "Qty: ${item.quantity} ${item.unit} x ${String.format("%.2f", item.pricePerUnit)}"
        holder.textSubtotal.text = "Subtotal: ${String.format("%.2f", item.getSubtotal())}"

        holder.btnRemoveItem.setOnClickListener {
            onItemRemoved(item)
        }
    }

    override fun getItemCount() = items.size

    fun addItem(item: SaleItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun removeItem(item: SaleItem) {
        val index = items.indexOf(item)
        if (index >= 0) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun getTotalAmount(): Double {
        return items.sumOf { it.getSubtotal() }
    }

    fun getTotalQuantityForProduct(productId: Long): Double {
        return items.filter { it.productId == productId }.sumOf { it.quantity }
    }

    fun getItems(): List<SaleItem> {
        return items.toList()
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }
}
