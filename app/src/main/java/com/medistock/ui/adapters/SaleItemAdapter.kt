package com.medistock.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.data.entities.SaleItem

class SaleItemAdapter(
    private val items: MutableList<SaleItem>,
    private val onRemove: (SaleItem) -> Unit
) : RecyclerView.Adapter<SaleItemAdapter.SaleItemViewHolder>() {

    class SaleItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textItemProductName: TextView = itemView.findViewById(R.id.textItemProductName)
        val textItemQuantity: TextView = itemView.findViewById(R.id.textItemQuantity)
        val textItemPrice: TextView = itemView.findViewById(R.id.textItemPrice)
        val textItemSubtotal: TextView = itemView.findViewById(R.id.textItemSubtotal)
        val btnRemoveItem: Button = itemView.findViewById(R.id.btnRemoveItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaleItemViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sale_item, parent, false)
        return SaleItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: SaleItemViewHolder, position: Int) {
        val item = items[position]

        holder.textItemProductName.text = item.productName
        holder.textItemQuantity.text = "Quantity: ${item.quantity} ${item.unit}"
        holder.textItemPrice.text = String.format("@%.2f", item.pricePerUnit)
        holder.textItemSubtotal.text = String.format("%.2f", item.subtotal)

        holder.btnRemoveItem.setOnClickListener { onRemove(item) }
    }

    override fun getItemCount(): Int = items.size

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

    fun getItems(): List<SaleItem> = items

    fun getTotalQuantityForProduct(productId: Long): Double {
        return items.filter { it.productId == productId }.sumOf { it.quantity }
    }

    fun getTotalAmount(): Double {
        return items.sumOf { it.subtotal }
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }
}
