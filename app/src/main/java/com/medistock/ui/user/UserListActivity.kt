package com.medistock.ui.user

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.medistock.R
import com.medistock.data.db.AppDatabase
import com.medistock.ui.adapters.UserAdapter
import com.medistock.util.AuthManager
import kotlinx.coroutines.launch

class UserListActivity : AppCompatActivity() {
    private lateinit var adapter: UserAdapter
    private lateinit var db: AppDatabase
    private lateinit var authManager: AuthManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authManager = AuthManager.getInstance(this)

        // Check if user is admin
        if (!authManager.isAdmin()) {
            Toast.makeText(this, "Accès refusé : Administrateur uniquement", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setContentView(R.layout.activity_user_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Gestion des utilisateurs"

        db = AppDatabase.getInstance(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerUsers)
        val btnAdd = findViewById<Button>(R.id.btnAddUser)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = UserAdapter { user ->
            val intent = Intent(this, UserAddEditActivity::class.java)
            intent.putExtra("USER_ID", user.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        btnAdd.setOnClickListener {
            startActivity(Intent(this, UserAddEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        loadUsers()
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            try {
                val users = db.userDao().getAllUsers()
                adapter.submitList(users)
            } catch (e: Exception) {
                Toast.makeText(this@UserListActivity, "Erreur: ${e.message}", Toast.LENGTH_SHORT).show()
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
