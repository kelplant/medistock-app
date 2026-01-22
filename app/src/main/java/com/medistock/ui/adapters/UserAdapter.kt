package com.medistock.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.shared.domain.model.User

class UserAdapter(
    private val onClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var users = listOf<User>()

    fun submitList(list: List<User>) {
        users = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount() = users.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user)
        holder.itemView.setOnClickListener { onClick(user) }
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textUserFullName: TextView = itemView.findViewById(R.id.textUserFullName)
        private val textUsername: TextView = itemView.findViewById(R.id.textUsername)
        private val textUserRole: TextView = itemView.findViewById(R.id.textUserRole)
        private val textUserStatus: TextView = itemView.findViewById(R.id.textUserStatus)

        fun bind(user: User) {
            textUserFullName.text = user.fullName
            textUsername.text = "@${user.username}"

            if (user.isAdmin) {
                textUserRole.text = "Admin"
                textUserRole.setBackgroundColor(Color.parseColor("#4CAF50"))
            } else {
                textUserRole.text = "User"
                textUserRole.setBackgroundColor(Color.parseColor("#2196F3"))
            }

            if (user.isActive) {
                textUserStatus.text = "Actif"
                textUserStatus.setTextColor(Color.parseColor("#4CAF50"))
            } else {
                textUserStatus.text = "Inactif"
                textUserStatus.setTextColor(Color.parseColor("#D32F2F"))
            }
        }
    }
}
