package com.medistock.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.data.entities.Product

class StockAdapter(private var products: List<Product>) :
    RecyclerView.Adapter<StockAdapter.StockViewHolder>() {

    inner class StockViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.textProductName)
        val unitText: TextView = view.findViewById(R.id.textProductUnit)
        // Ajoute d’autres vues si besoin, comme quantité
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StockViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_stock, parent, false)
        return StockViewHolder(view)
    }

    override fun onBindViewHolder(holder: StockViewHolder, position: Int) {
        val product = products[position]
        holder.nameText.text = product.name
        holder.unitText.text = product.unit
    }

    override fun getItemCount() = products.size

    fun updateData(newProducts: List<Product>) {
        products = newProducts
        notifyDataSetChanged()
    }
}