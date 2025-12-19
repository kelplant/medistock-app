package com.medistock.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.data.entities.SaleWithItems
import java.text.SimpleDateFormat
import java.util.*

class SaleAdapter(
    private val onEdit: (SaleWithItems) -> Unit,
    private val onDelete: (SaleWithItems) -> Unit
) : RecyclerView.Adapter<SaleAdapter.SaleViewHolder>() {

    private var sales = listOf<SaleWithItems>()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    fun submitList(list: List<SaleWithItems>) {
        sales = list
        notifyDataSetChanged()
    }

    class SaleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textSaleCustomer: TextView = itemView.findViewById(R.id.textSaleCustomer)
        val textSaleDate: TextView = itemView.findViewById(R.id.textSaleDate)
        val textSaleItems: TextView = itemView.findViewById(R.id.textSaleItems)
        val textSaleTotal: TextView = itemView.findViewById(R.id.textSaleTotal)
        val btnEditSale: Button = itemView.findViewById(R.id.btnEditSale)
        val btnDeleteSale: Button = itemView.findViewById(R.id.btnDeleteSale)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SaleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sale, parent, false)
        return SaleViewHolder(view)
    }

    override fun onBindViewHolder(holder: SaleViewHolder, position: Int) {
        val saleWithItems = sales[position]
        val sale = saleWithItems.sale

        holder.textSaleCustomer.text = sale.customerName
        holder.textSaleDate.text = dateFormat.format(Date(sale.date))

        val itemCount = saleWithItems.items.size
        holder.textSaleItems.text = if (itemCount == 1) "1 item" else "$itemCount items"

        holder.textSaleTotal.text = String.format("%.2f", sale.totalAmount)

        holder.btnEditSale.setOnClickListener { onEdit(saleWithItems) }
        holder.btnDeleteSale.setOnClickListener { onDelete(saleWithItems) }
    }

    override fun getItemCount(): Int = sales.size
}
