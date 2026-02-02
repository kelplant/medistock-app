package com.medistock.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.shared.domain.model.ProductWithCategory

class ProductAdapter(
    private val onClick: (ProductWithCategory) -> Unit
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    private var products = listOf<ProductWithCategory>()

    fun submitList(list: List<ProductWithCategory>) {
        products = list
        notifyDataSetChanged()
    }

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textProductName: TextView = itemView.findViewById(R.id.textProductName)
        val textProductCategory: TextView = itemView.findViewById(R.id.textProductCategory)
        val textProductDescription: TextView = itemView.findViewById(R.id.textProductDescription)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = products[position]
        holder.textProductName.text = product.name
        holder.textProductCategory.text = "${product.unit} - ${product.unitVolume ?: 1.0}"
        holder.textProductDescription.text = product.description.orEmpty()
        holder.itemView.setOnClickListener { onClick(product) }
    }

    override fun getItemCount(): Int = products.size
}
