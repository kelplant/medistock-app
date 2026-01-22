package com.medistock.ui.packaging

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.shared.domain.model.PackagingType

class PackagingTypeAdapter(
    private val onEdit: (PackagingType) -> Unit,
    private val onDelete: (PackagingType) -> Unit,
    private val onToggleActive: (PackagingType) -> Unit
) : RecyclerView.Adapter<PackagingTypeAdapter.PackagingTypeViewHolder>() {

    private var packagingTypes = listOf<PackagingType>()

    fun submitList(list: List<PackagingType>) {
        packagingTypes = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackagingTypeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_packaging_type, parent, false)
        return PackagingTypeViewHolder(view)
    }

    override fun getItemCount() = packagingTypes.size

    override fun onBindViewHolder(holder: PackagingTypeViewHolder, position: Int) {
        val packagingType = packagingTypes[position]
        holder.bind(packagingType, onEdit, onDelete, onToggleActive)
    }

    class PackagingTypeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textName: TextView = itemView.findViewById(R.id.textPackagingTypeName)
        private val textLevels: TextView = itemView.findViewById(R.id.textPackagingTypeLevels)
        private val textConversion: TextView = itemView.findViewById(R.id.textPackagingTypeConversion)
        private val textStatus: TextView = itemView.findViewById(R.id.textPackagingTypeStatus)
        private val btnEdit: Button = itemView.findViewById(R.id.btnEditPackagingType)
        private val btnDelete: Button = itemView.findViewById(R.id.btnDeletePackagingType)
        private val btnToggleActive: Button = itemView.findViewById(R.id.btnToggleActivePackagingType)

        fun bind(
            packagingType: PackagingType,
            onEdit: (PackagingType) -> Unit,
            onDelete: (PackagingType) -> Unit,
            onToggleActive: (PackagingType) -> Unit
        ) {
            textName.text = packagingType.name

            // Afficher les niveaux
            val levelsText = if (packagingType.hasTwoLevels()) {
                "Niveaux: ${packagingType.level1Name} / ${packagingType.level2Name}"
            } else {
                "Niveau unique: ${packagingType.level1Name}"
            }
            textLevels.text = levelsText

            // Afficher le facteur de conversion
            if (packagingType.defaultConversionFactor != null) {
                textConversion.text = "Conversion: ${packagingType.defaultConversionFactor} ${packagingType.level2Name}/${packagingType.level1Name}"
                textConversion.visibility = View.VISIBLE
            } else {
                textConversion.visibility = View.GONE
            }

            // Statut
            textStatus.text = if (packagingType.isActive) "Actif" else "Inactif"
            textStatus.setTextColor(
                if (packagingType.isActive) {
                    itemView.context.getColor(android.R.color.holo_green_dark)
                } else {
                    itemView.context.getColor(android.R.color.holo_red_dark)
                }
            )

            // Couleur de fond selon le statut
            itemView.alpha = if (packagingType.isActive) 1.0f else 0.5f

            // Boutons
            btnEdit.setOnClickListener { onEdit(packagingType) }
            btnDelete.setOnClickListener { onDelete(packagingType) }
            btnToggleActive.text = if (packagingType.isActive) "Deactivate" else "Activate"
            btnToggleActive.setOnClickListener { onToggleActive(packagingType) }
        }
    }
}
