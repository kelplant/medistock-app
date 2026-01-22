package com.medistock.ui.admin

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.ui.adapters.AuditHistoryAdapter
import com.medistock.ui.viewmodel.AuditHistoryViewModel
import com.medistock.util.AuthManager
import kotlinx.coroutines.launch

class AuditHistoryActivity : AppCompatActivity() {
    private lateinit var adapter: AuditHistoryAdapter
    private lateinit var viewModel: AuditHistoryViewModel
    private lateinit var authManager: AuthManager
    private lateinit var textAuditCount: TextView
    private lateinit var textEmptyState: TextView
    private lateinit var recyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = AuthManager.getInstance(this)

        // Check if user is admin
        if (!authManager.isAdmin()) {
            Toast.makeText(this, "Access denied: Administrators only", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_audit_history)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Audit History"

        // Initialize views
        recyclerView = findViewById(R.id.recyclerAuditHistory)
        textAuditCount = findViewById(R.id.textAuditCount)
        textEmptyState = findViewById(R.id.textEmptyState)

        // Initialize ViewModel
        viewModel = ViewModelProvider(this)[AuditHistoryViewModel::class.java]

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = AuditHistoryAdapter()
        recyclerView.adapter = adapter

        // Observe audit entries
        lifecycleScope.launch {
            viewModel.auditEntries.collect { entries ->
                adapter.submitList(entries)

                // Update count
                textAuditCount.text = "Total entries: ${entries.size}"

                // Show/hide empty state
                if (entries.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    textEmptyState.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    textEmptyState.visibility = View.GONE
                }
            }
        }

        // Observe total count
        lifecycleScope.launch {
            viewModel.totalCount.collect { count ->
                // Could be used for pagination info
                // Total: $count entries
            }
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
