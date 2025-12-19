package com.medistock.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.data.entities.Customer

class CustomerAdapter(
    private val onClick: (Customer) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.CustomerViewHolder>() {

    private var customers = listOf<Customer>()

    fun submitList(list: List<Customer>) {
        customers = list
        notifyDataSetChanged()
    }

    class CustomerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textCustomerName: TextView = itemView.findViewById(R.id.textCustomerName)
        val textCustomerPhone: TextView = itemView.findViewById(R.id.textCustomerPhone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_customer, parent, false)
        return CustomerViewHolder(view)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        val customer = customers[position]
        holder.textCustomerName.text = customer.name
        holder.textCustomerPhone.text = customer.phone ?: "No phone"
        holder.itemView.setOnClickListener { onClick(customer) }
    }

    override fun getItemCount(): Int = customers.size
}
