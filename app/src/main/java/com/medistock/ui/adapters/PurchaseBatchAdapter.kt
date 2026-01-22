package com.medistock.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.data.entities.PurchaseBatch
import java.text.SimpleDateFormat
import java.util.Locale

class PurchaseBatchAdapter(
    private val productNames: Map<String, String>,
    private val onClick: (PurchaseBatch) -> Unit
) : RecyclerView.Adapter<PurchaseBatchAdapter.PurchaseBatchViewHolder>() {

    private var batches = listOf<PurchaseBatch>()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun submitList(list: List<PurchaseBatch>) {
        batches = list
        notifyDataSetChanged()
    }

    class PurchaseBatchViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textProductName: TextView = itemView.findViewById(R.id.textProductName)
        val textBatchStatus: TextView = itemView.findViewById(R.id.textBatchStatus)
        val textBatchNumber: TextView = itemView.findViewById(R.id.textBatchNumber)
        val textPurchaseDate: TextView = itemView.findViewById(R.id.textPurchaseDate)
        val textQuantityInfo: TextView = itemView.findViewById(R.id.textQuantityInfo)
        val textPurchasePrice: TextView = itemView.findViewById(R.id.textPurchasePrice)
        val textSupplier: TextView = itemView.findViewById(R.id.textSupplier)
        val textExpiryDate: TextView = itemView.findViewById(R.id.textExpiryDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PurchaseBatchViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_purchase_batch, parent, false)
        return PurchaseBatchViewHolder(view)
    }

    override fun onBindViewHolder(holder: PurchaseBatchViewHolder, position: Int) {
        val batch = batches[position]

        holder.textProductName.text = productNames[batch.productId] ?: "Unknown Product"
        holder.textBatchNumber.text = batch.batchNumber?.let { "Batch #$it" } ?: "No batch number"
        holder.textPurchaseDate.text = dateFormat.format(batch.purchaseDate)
        holder.textQuantityInfo.text = "Qty: ${batch.remainingQuantity.toInt()}/${batch.initialQuantity.toInt()}"
        holder.textPurchasePrice.text = "${batch.purchasePrice.toInt()} FCFA/unit"

        if (batch.supplierName.isNotBlank()) {
            holder.textSupplier.text = "Supplier: ${batch.supplierName}"
            holder.textSupplier.visibility = View.VISIBLE
        } else {
            holder.textSupplier.visibility = View.GONE
        }

        batch.expiryDate?.let { expiryDate ->
            holder.textExpiryDate.text = "Expires: ${dateFormat.format(expiryDate)}"
            holder.textExpiryDate.visibility = View.VISIBLE
            // Check if expiring soon (within 30 days)
            val thirtyDaysFromNow = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)
            if (expiryDate <= System.currentTimeMillis()) {
                holder.textExpiryDate.setTextColor(0xFFF44336.toInt()) // Red - expired
            } else if (expiryDate <= thirtyDaysFromNow) {
                holder.textExpiryDate.setTextColor(0xFFFF9800.toInt()) // Orange - expiring soon
            } else {
                holder.textExpiryDate.setTextColor(0xFF757575.toInt()) // Grey - normal
            }
        } ?: run {
            holder.textExpiryDate.visibility = View.GONE
        }

        // Status indicator
        if (batch.isExhausted || batch.remainingQuantity <= 0) {
            holder.textBatchStatus.text = "Exhausted"
            holder.textBatchStatus.setTextColor(0xFF9E9E9E.toInt()) // Grey
        } else {
            holder.textBatchStatus.text = "Active"
            holder.textBatchStatus.setTextColor(0xFF4CAF50.toInt()) // Green
        }

        holder.itemView.setOnClickListener { onClick(batch) }
    }

    override fun getItemCount(): Int = batches.size
}
