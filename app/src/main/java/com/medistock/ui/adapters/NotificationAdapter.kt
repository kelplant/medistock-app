package com.medistock.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.shared.domain.notification.NotificationEvent
import com.medistock.shared.domain.notification.NotificationPriority
import java.util.concurrent.TimeUnit

class NotificationAdapter(
    private val onDismissClick: (NotificationEvent) -> Unit,
    private val onItemClick: (NotificationEvent) -> Unit
) : ListAdapter<NotificationEvent, NotificationAdapter.NotificationViewHolder>(NotificationDiffCallback()) {

    companion object {
        // Pre-computed colors for performance (avoid parsing on each bind)
        private val COLOR_CRITICAL = Color.parseColor("#F44336")
        private val COLOR_HIGH = Color.parseColor("#FF9800")
        private val COLOR_MEDIUM = Color.parseColor("#2196F3")
        private val COLOR_LOW = Color.parseColor("#9E9E9E")
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(view, onDismissClick, onItemClick)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class NotificationViewHolder(
        itemView: View,
        private val onDismissClick: (NotificationEvent) -> Unit,
        private val onItemClick: (NotificationEvent) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        private val priorityIndicator: View = itemView.findViewById(R.id.priorityIndicator)
        private val textTitle: TextView = itemView.findViewById(R.id.textTitle)
        private val textPriority: TextView = itemView.findViewById(R.id.textPriority)
        private val textMessage: TextView = itemView.findViewById(R.id.textMessage)
        private val textDate: TextView = itemView.findViewById(R.id.textDate)
        private val buttonDismiss: ImageButton = itemView.findViewById(R.id.buttonDismiss)

        fun bind(event: NotificationEvent) {
            textTitle.text = event.title
            textMessage.text = event.message
            textDate.text = formatRelativeTime(event.createdAt)

            // Priority styling
            val (color, label) = when (event.priority) {
                NotificationPriority.CRITICAL -> COLOR_CRITICAL to "CRITIQUE"
                NotificationPriority.HIGH -> COLOR_HIGH to "URGENT"
                NotificationPriority.MEDIUM -> COLOR_MEDIUM to "INFO"
                NotificationPriority.LOW -> COLOR_LOW to "FAIBLE"
            }

            priorityIndicator.setBackgroundColor(color)
            textPriority.setBackgroundColor(color)
            textPriority.text = label

            buttonDismiss.setOnClickListener { onDismissClick(event) }
            itemView.setOnClickListener { onItemClick(event) }
        }

        private fun formatRelativeTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "À l'instant"
                diff < TimeUnit.HOURS.toMillis(1) -> {
                    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
                    "Il y a $minutes min"
                }
                diff < TimeUnit.DAYS.toMillis(1) -> {
                    val hours = TimeUnit.MILLISECONDS.toHours(diff)
                    "Il y a $hours h"
                }
                diff < TimeUnit.DAYS.toMillis(7) -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "Il y a $days jour(s)"
                }
                else -> {
                    val days = TimeUnit.MILLISECONDS.toDays(diff)
                    "Il y a $days jours"
                }
            }
        }
    }

    private class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationEvent>() {
        override fun areItemsTheSame(oldItem: NotificationEvent, newItem: NotificationEvent): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationEvent, newItem: NotificationEvent): Boolean {
            return oldItem == newItem
        }
    }
}
