package com.medistock.ui.transfer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.shared.domain.model.PackagingType
import com.medistock.shared.domain.model.Product
import com.medistock.shared.domain.model.ProductTransfer
import com.medistock.shared.domain.model.Site
import java.text.SimpleDateFormat
import java.util.*

class TransferAdapter(
    private val transfers: List<ProductTransfer>,
    private val products: Map<String, Product>,
    private val packagingTypes: Map<String, PackagingType>,
    private val sites: Map<String, Site>,
    private val onEditClick: (ProductTransfer) -> Unit,
    private val onDeleteClick: (ProductTransfer) -> Unit
) : RecyclerView.Adapter<TransferAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    private fun getUnit(product: Product?): String {
        if (product == null) return ""
        val packagingType = packagingTypes[product.packagingTypeId]
        return packagingType?.getLevelName(product.selectedLevel) ?: ""
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textProduct: TextView = view.findViewById(R.id.textProduct)
        val textQuantity: TextView = view.findViewById(R.id.textQuantity)
        val textRoute: TextView = view.findViewById(R.id.textRoute)
        val textDate: TextView = view.findViewById(R.id.textDate)
        val textCreatedBy: TextView = view.findViewById(R.id.textCreatedBy)
        val textNotes: TextView = view.findViewById(R.id.textNotes)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEditTransfer)
        val btnDelete: ImageButton = view.findViewById(R.id.btnDeleteTransfer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_transfer, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val transfer = transfers[position]
        val product = products[transfer.productId]
        val fromSite = sites[transfer.fromSiteId]
        val toSite = sites[transfer.toSiteId]

        holder.textProduct.text = product?.name ?: "Unknown Product"
        holder.textQuantity.text = "Quantity: ${transfer.quantity} ${getUnit(product)}"
        holder.textRoute.text = "From: ${fromSite?.name ?: "Unknown"} → To: ${toSite?.name ?: "Unknown"}"
        holder.textDate.text = "Date: ${dateFormat.format(Date(transfer.createdAt))}"
        holder.textCreatedBy.text = "By: ${transfer.createdBy.ifEmpty { "Unknown" }}"

        // Show notes if available
        val notes = transfer.notes
        if (!notes.isNullOrEmpty()) {
            holder.textNotes.visibility = View.VISIBLE
            holder.textNotes.text = "Note: $notes"
        } else {
            holder.textNotes.visibility = View.GONE
        }

        holder.btnEdit.setOnClickListener {
            onEditClick(transfer)
        }

        holder.btnDelete.setOnClickListener {
            onDeleteClick(transfer)
        }
    }

    override fun getItemCount() = transfers.size
}
