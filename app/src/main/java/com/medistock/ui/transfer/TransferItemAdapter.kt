package com.medistock.ui.transfer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R

class TransferItemAdapter(
    private val items: MutableList<TransferItem>,
    private val onItemRemoved: (TransferItem) -> Unit
) : RecyclerView.Adapter<TransferItemAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textProductName: TextView = view.findViewById(R.id.textProductName)
        val textQuantity: TextView = view.findViewById(R.id.textQuantity)
        val btnRemoveItem: ImageButton = view.findViewById(R.id.btnRemoveItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transfer_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.textProductName.text = item.productName
        holder.textQuantity.text = "Quantity: ${item.quantity} ${item.unit}"

        holder.btnRemoveItem.setOnClickListener {
            onItemRemoved(item)
        }
    }

    override fun getItemCount() = items.size

    fun addItem(item: TransferItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun removeItem(item: TransferItem) {
        val index = items.indexOf(item)
        if (index >= 0) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun getTotalQuantityForProduct(productId: Long): Double {
        return items.filter { it.productId == productId }.sumOf { it.quantity }
    }

    fun getItems(): List<TransferItem> {
        return items.toList()
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }
}
