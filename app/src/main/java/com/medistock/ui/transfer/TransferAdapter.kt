package com.medistock.ui.transfer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.data.entities.Product
import com.medistock.data.entities.ProductTransfer
import com.medistock.data.entities.Site
import java.text.SimpleDateFormat
import java.util.*

class TransferAdapter(
    private val transfers: List<ProductTransfer>,
    private val products: Map<Long, Product>,
    private val sites: Map<Long, Site>,
    private val onEditClick: (ProductTransfer) -> Unit,
    private val onDeleteClick: (ProductTransfer) -> Unit
) : RecyclerView.Adapter<TransferAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

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
        holder.textQuantity.text = "Quantity: ${transfer.quantity} ${product?.unit ?: ""}"
        holder.textRoute.text = "From: ${fromSite?.name ?: "Unknown"} → To: ${toSite?.name ?: "Unknown"}"
        holder.textDate.text = "Date: ${dateFormat.format(Date(transfer.date))}"
        holder.textCreatedBy.text = "By: ${transfer.createdBy.ifEmpty { "Unknown" }}"

        // Show notes if available
        if (transfer.notes.isNotEmpty()) {
            holder.textNotes.visibility = View.VISIBLE
            holder.textNotes.text = "Note: ${transfer.notes}"
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
