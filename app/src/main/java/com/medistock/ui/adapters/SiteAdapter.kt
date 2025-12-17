package com.medistock.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medistock.data.entities.Site
import com.medistock.databinding.ItemSiteBinding

class SiteAdapter(
    private val onClick: (Site) -> Unit
) : RecyclerView.Adapter<SiteAdapter.SiteViewHolder>() {

    private var sites = listOf<Site>()

    fun submitList(list: List<Site>) {
        sites = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteViewHolder {
        val binding = ItemSiteBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SiteViewHolder(binding)
    }

    override fun getItemCount() = sites.size

    override fun onBindViewHolder(holder: SiteViewHolder, position: Int) {
        val site = sites[position]
        holder.binding.textSiteName.text = site.name
        holder.itemView.setOnClickListener { onClick(site) }
    }

    class SiteViewHolder(val binding: ItemSiteBinding) :
        RecyclerView.ViewHolder(binding.root)
}
