package com.medistock.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.data.entities.Inventory
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

class InventoryAdapter(
    private val productNames: Map<String, String>,
    private val onClick: (Inventory) -> Unit
) : RecyclerView.Adapter<InventoryAdapter.InventoryViewHolder>() {

    private var inventories = listOf<Inventory>()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun submitList(list: List<Inventory>) {
        inventories = list
        notifyDataSetChanged()
    }

    class InventoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textProductName: TextView = itemView.findViewById(R.id.textProductName)
        val textCountDate: TextView = itemView.findViewById(R.id.textCountDate)
        val textTheoreticalQty: TextView = itemView.findViewById(R.id.textTheoreticalQty)
        val textCountedQty: TextView = itemView.findViewById(R.id.textCountedQty)
        val textDiscrepancy: TextView = itemView.findViewById(R.id.textDiscrepancy)
        val textCountedBy: TextView = itemView.findViewById(R.id.textCountedBy)
        val textReason: TextView = itemView.findViewById(R.id.textReason)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InventoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_inventory, parent, false)
        return InventoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: InventoryViewHolder, position: Int) {
        val inventory = inventories[position]

        holder.textProductName.text = productNames[inventory.productId] ?: "Unknown Product"
        holder.textCountDate.text = dateFormat.format(inventory.countDate)
        holder.textTheoreticalQty.text = "Theoretical: ${inventory.theoreticalQuantity.toInt()}"
        holder.textCountedQty.text = "Counted: ${inventory.countedQuantity.toInt()}"

        // Discrepancy display with color
        val discrepancy = inventory.discrepancy
        when {
            discrepancy > 0 -> {
                holder.textDiscrepancy.text = "Discrepancy: +${discrepancy.toInt()}"
                holder.textDiscrepancy.setTextColor(0xFF4CAF50.toInt()) // Green - more than expected
            }
            discrepancy < 0 -> {
                holder.textDiscrepancy.text = "Discrepancy: ${discrepancy.toInt()}"
                holder.textDiscrepancy.setTextColor(0xFFF44336.toInt()) // Red - less than expected
            }
            else -> {
                holder.textDiscrepancy.text = "Discrepancy: 0"
                holder.textDiscrepancy.setTextColor(0xFF757575.toInt()) // Grey - no discrepancy
            }
        }

        // Counted by
        if (inventory.countedBy.isNotBlank()) {
            holder.textCountedBy.text = "By: ${inventory.countedBy}"
            holder.textCountedBy.visibility = View.VISIBLE
        } else {
            holder.textCountedBy.visibility = View.GONE
        }

        // Reason (only show if there's a discrepancy and a reason)
        if (inventory.reason.isNotBlank() && abs(discrepancy) > 0) {
            holder.textReason.text = "Reason: ${inventory.reason}"
            holder.textReason.visibility = View.VISIBLE
        } else {
            holder.textReason.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onClick(inventory) }
    }

    override fun getItemCount(): Int = inventories.size
}
