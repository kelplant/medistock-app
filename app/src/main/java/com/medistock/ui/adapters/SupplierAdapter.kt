package com.medistock.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.shared.domain.model.Supplier
import com.medistock.shared.i18n.L

class SupplierAdapter(
    private val onClick: (Supplier) -> Unit
) : RecyclerView.Adapter<SupplierAdapter.SupplierViewHolder>() {

    private var suppliers = listOf<Supplier>()

    fun submitList(list: List<Supplier>) {
        suppliers = list
        notifyDataSetChanged()
    }

    class SupplierViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textSupplierName: TextView = itemView.findViewById(R.id.textSupplierName)
        val textSupplierPhone: TextView = itemView.findViewById(R.id.textSupplierPhone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SupplierViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_supplier, parent, false)
        return SupplierViewHolder(view)
    }

    override fun onBindViewHolder(holder: SupplierViewHolder, position: Int) {
        val supplier = suppliers[position]
        holder.textSupplierName.text = supplier.name
        holder.textSupplierPhone.text = supplier.phone ?: L.strings.phone
        holder.itemView.setOnClickListener { onClick(supplier) }
    }

    override fun getItemCount(): Int = suppliers.size
}
