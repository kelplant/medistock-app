package com.medistock.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.shared.domain.model.AuditHistory
import java.text.SimpleDateFormat
import java.util.*

class AuditHistoryAdapter : RecyclerView.Adapter<AuditHistoryAdapter.AuditHistoryViewHolder>() {

    private var entries = listOf<AuditHistory>()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun submitList(list: List<AuditHistory>) {
        entries = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AuditHistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_audit_history, parent, false)
        return AuditHistoryViewHolder(view)
    }

    override fun getItemCount() = entries.size

    override fun onBindViewHolder(holder: AuditHistoryViewHolder, position: Int) {
        val entry = entries[position]
        holder.bind(entry, dateFormat)
    }

    class AuditHistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textEntityInfo: TextView = itemView.findViewById(R.id.textEntityInfo)
        private val textActionType: TextView = itemView.findViewById(R.id.textActionType)
        private val textChangeDetails: TextView = itemView.findViewById(R.id.textChangeDetails)
        private val textUserAndDate: TextView = itemView.findViewById(R.id.textUserAndDate)

        fun bind(entry: AuditHistory, dateFormat: SimpleDateFormat) {
            // Entity info
            textEntityInfo.text = "${entry.entityType} #${entry.entityId}"

            // Action type with color coding
            textActionType.text = entry.actionType
            when (entry.actionType) {
                "INSERT" -> {
                    textActionType.setBackgroundColor(Color.parseColor("#4CAF50"))
                    textActionType.setTextColor(Color.WHITE)
                }
                "UPDATE" -> {
                    textActionType.setBackgroundColor(Color.parseColor("#FF9800"))
                    textActionType.setTextColor(Color.WHITE)
                }
                "DELETE" -> {
                    textActionType.setBackgroundColor(Color.parseColor("#F44336"))
                    textActionType.setTextColor(Color.WHITE)
                }
                else -> {
                    textActionType.setBackgroundColor(Color.parseColor("#9E9E9E"))
                    textActionType.setTextColor(Color.WHITE)
                }
            }

            // Change details
            val changeText = buildChangeDetailsText(entry)
            textChangeDetails.text = changeText

            // User and date
            val dateStr = dateFormat.format(Date(entry.changedAt))
            textUserAndDate.text = "By ${entry.changedBy} at $dateStr"
        }

        private fun buildChangeDetailsText(entry: AuditHistory): String {
            return when (entry.actionType) {
                "INSERT" -> {
                    "New entry: ${entry.newValue ?: ""}"
                }
                "UPDATE" -> {
                    if (entry.fieldName != null) {
                        "${entry.fieldName}: ${entry.oldValue ?: "null"} → ${entry.newValue ?: "null"}"
                    } else {
                        entry.description ?: "Updated"
                    }
                }
                "DELETE" -> {
                    "Deleted: ${entry.oldValue ?: ""}"
                }
                else -> {
                    entry.description ?: "Action performed"
                }
            }
        }
    }
}
