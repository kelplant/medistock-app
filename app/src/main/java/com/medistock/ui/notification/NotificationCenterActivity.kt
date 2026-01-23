package com.medistock.ui.notification

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medistock.MedistockApplication
import com.medistock.R
import com.medistock.shared.domain.notification.NotificationEvent
import com.medistock.ui.adapters.NotificationAdapter
import com.medistock.ui.stock.StockListActivity
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Activity displaying the notification center with all undismissed notifications.
 * Users can view, dismiss individual notifications, or dismiss all at once.
 */
class NotificationCenterActivity : AppCompatActivity() {

    private val sdk = MedistockApplication.sdk

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var textNotificationCount: TextView
    private lateinit var buttonDismissAll: Button
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_center)

        supportActionBar?.apply {
            title = "Notifications"
            setDisplayHomeAsUpEnabled(true)
        }

        initViews()
        setupAdapter()
        observeNotifications()
    }

    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerNotifications)
        emptyStateContainer = findViewById(R.id.emptyStateContainer)
        textNotificationCount = findViewById(R.id.textNotificationCount)
        buttonDismissAll = findViewById(R.id.buttonDismissAll)

        buttonDismissAll.setOnClickListener {
            dismissAllNotifications()
        }
    }

    private fun setupAdapter() {
        adapter = NotificationAdapter(
            onDismissClick = { event -> dismissNotification(event) },
            onItemClick = { event -> navigateToDeepLink(event) }
        )

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationCenterActivity)
            adapter = this@NotificationCenterActivity.adapter
        }
    }

    private fun observeNotifications() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                sdk.notificationRepository.observeUndismissed().collectLatest { notifications ->
                    updateUI(notifications)
                }
            }
        }
    }

    private fun updateUI(notifications: List<NotificationEvent>) {
        adapter.submitList(notifications)

        val count = notifications.size
        textNotificationCount.text = "$count notification(s)"

        if (notifications.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyStateContainer.visibility = View.VISIBLE
            buttonDismissAll.visibility = View.GONE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateContainer.visibility = View.GONE
            buttonDismissAll.visibility = View.VISIBLE
        }
    }

    private fun dismissNotification(event: NotificationEvent) {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    sdk.notificationRepository.markAsDismissed(event.id)
                }
            } catch (e: Exception) {
                Toast.makeText(this@NotificationCenterActivity, "Erreur lors de l'acquittement", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun dismissAllNotifications() {
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    sdk.notificationRepository.dismissAll()
                }
            } catch (e: Exception) {
                Toast.makeText(this@NotificationCenterActivity, "Erreur lors de l'acquittement", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToDeepLink(event: NotificationEvent) {
        val deepLink = event.deepLink ?: return

        // Parse the deep link: medistock://stock/{productId}
        if (deepLink.startsWith("medistock://stock/")) {
            val productId = deepLink.removePrefix("medistock://stock/")

            // Security: Validate UUID format before navigation
            if (!isValidUUID(productId)) {
                return
            }

            val intent = Intent(this, StockListActivity::class.java).apply {
                putExtra("highlight_product_id", productId)
            }
            startActivity(intent)
        }
    }

    private fun isValidUUID(value: String): Boolean {
        return try {
            UUID.fromString(value)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
