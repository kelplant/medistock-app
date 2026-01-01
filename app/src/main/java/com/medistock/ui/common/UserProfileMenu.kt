package com.medistock.ui.common

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.PopupMenu
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.toColorInt
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.medistock.R
import com.medistock.data.remote.SupabaseClientProvider
import com.medistock.ui.auth.ChangePasswordActivity
import com.medistock.ui.auth.LoginActivity
import com.medistock.util.AuthManager
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.WeakHashMap

/**
 * Menu global affichant un badge rond avec l'initiale de l'utilisateur
 * et l'état Realtime (vert connecté, rouge déconnecté).
 * Cliquer sur le badge ouvre un menu permettant de voir le profil,
 * changer le mot de passe et se déconnecter.
 */
object UserProfileMenu {

    private val attached = WeakHashMap<AppCompatActivity, MenuProvider>()

    fun attach(activity: AppCompatActivity) {
        // Évite les doublons lors des recréations
        if (attached.containsKey(activity)) return

        val provider = object : MenuProvider {
            private var statusJob: Job? = null

            override fun onCreateMenu(menu: Menu, menuInflater: android.view.MenuInflater) {
                if (menu.findItem(R.id.menu_profile_badge) != null) return

                val item = menu.add(Menu.NONE, R.id.menu_profile_badge, Menu.NONE, "Profil")
                item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)

                val actionView = LayoutInflater.from(activity)
                    .inflate(R.layout.menu_profile_badge, null)
                val badge = actionView.findViewById<TextView>(R.id.tvProfileBadge)
                item.actionView = actionView

                configureBadge(badge, activity)
                startRealtimeObservation(badge, activity)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean = false

            private fun configureBadge(badge: TextView, activity: AppCompatActivity) {
                val authManager = AuthManager.getInstance(activity)
                val fullName = authManager.getFullName()
                val initial = fullName.firstOrNull()?.uppercase() ?: "?"
                badge.text = initial
                applyStatus(badge, Realtime.Status.DISCONNECTED)

                badge.setOnClickListener {
                    showProfilePopup(activity, badge)
                }
            }

            private fun startRealtimeObservation(badge: TextView, activity: AppCompatActivity) {
                statusJob?.cancel()
                statusJob = activity.lifecycleScope.launch {
                    activity.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                        val statusFlow = runCatching { SupabaseClientProvider.client.realtime.status }.getOrNull()
                        if (statusFlow == null) {
                            applyStatus(badge, Realtime.Status.DISCONNECTED)
                            return@repeatOnLifecycle
                        }
                        applyStatus(badge, Realtime.Status.CONNECTING)
                        runCatching { SupabaseClientProvider.client.realtime.connect() }
                        statusFlow.collectLatest { status: Realtime.Status ->
                            applyStatus(badge, status)
                        }
                    }
                }
            }

            private fun showProfilePopup(activity: AppCompatActivity, anchor: TextView) {
                val authManager = AuthManager.getInstance(activity)
                val popup = PopupMenu(activity, anchor)
                popup.menu.add(Menu.NONE, R.id.menu_profile_info, Menu.NONE, authManager.getFullName())
                    .isEnabled = false
                popup.menu.add(Menu.NONE, R.id.menu_change_password, Menu.NONE, activity.getString(R.string.change_password))
                popup.menu.add(Menu.NONE, R.id.menu_logout, Menu.NONE, activity.getString(R.string.logout))

                popup.setOnMenuItemClickListener { item ->
                    when (item.itemId) {
                        R.id.menu_change_password -> {
                            activity.startActivity(Intent(activity, ChangePasswordActivity::class.java))
                            true
                        }
                        R.id.menu_logout -> {
                            showLogoutDialog(activity)
                            true
                        }
                        else -> false
                    }
                }
                popup.show()
            }
        }

        activity.addMenuProvider(provider, activity, Lifecycle.State.RESUMED)
        attached[activity] = provider
    }

    private fun applyStatus(badge: TextView, status: Realtime.Status) {
        val background = (badge.background?.mutate() as? GradientDrawable)
        when (status) {
            Realtime.Status.CONNECTED -> {
                background?.setStroke(3, "#4CAF50".toColorInt())
                background?.setColor("#E8F5E9".toColorInt())
                badge.setTextColor("#2E7D32".toColorInt())
                badge.contentDescription = "Realtime connecté"
            }
            Realtime.Status.CONNECTING -> {
                background?.setStroke(3, "#FFC107".toColorInt())
                background?.setColor("#FFF8E1".toColorInt())
                badge.setTextColor("#FF8F00".toColorInt())
                badge.contentDescription = "Connexion Realtime..."
            }
            Realtime.Status.DISCONNECTED -> {
                background?.setStroke(3, "#F44336".toColorInt())
                background?.setColor("#FFEBEE".toColorInt())
                badge.setTextColor("#C62828".toColorInt())
                badge.contentDescription = "Realtime déconnecté"
            }
        }
        badge.background = background
    }

    private fun showLogoutDialog(activity: AppCompatActivity) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.logout)
            .setMessage(R.string.logout_confirmation)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                AuthManager.getInstance(activity).logout()
                val intent = Intent(activity, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                activity.startActivity(intent)
                activity.finish()
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }
}
