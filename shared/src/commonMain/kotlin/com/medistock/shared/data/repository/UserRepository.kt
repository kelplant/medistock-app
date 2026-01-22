package com.medistock.shared.data.repository

import com.medistock.shared.db.MedistockDatabase
import com.medistock.shared.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val database: MedistockDatabase) {

    private val queries = database.medistockQueries

    suspend fun getAll(): List<User> = withContext(Dispatchers.Default) {
        queries.getAllUsers().executeAsList().map { it.toModel() }
    }

    suspend fun getById(id: String): User? = withContext(Dispatchers.Default) {
        queries.getUserById(id).executeAsOneOrNull()?.toModel()
    }

    suspend fun getByUsername(username: String): User? = withContext(Dispatchers.Default) {
        queries.getUserByUsername(username).executeAsOneOrNull()?.toModel()
    }

    suspend fun insert(user: User) = withContext(Dispatchers.Default) {
        queries.insertUser(
            id = user.id,
            username = user.username,
            password = user.password,
            full_name = user.fullName,
            is_admin = if (user.isAdmin) 1L else 0L,
            is_active = if (user.isActive) 1L else 0L,
            created_at = user.createdAt,
            updated_at = user.updatedAt,
            created_by = user.createdBy,
            updated_by = user.updatedBy
        )
    }

    suspend fun update(user: User) = withContext(Dispatchers.Default) {
        queries.updateUser(
            username = user.username,
            full_name = user.fullName,
            is_admin = if (user.isAdmin) 1L else 0L,
            is_active = if (user.isActive) 1L else 0L,
            updated_at = user.updatedAt,
            updated_by = user.updatedBy,
            id = user.id
        )
    }

    suspend fun updatePassword(userId: String, password: String, updatedAt: Long, updatedBy: String) = withContext(Dispatchers.Default) {
        queries.updateUserPassword(
            password = password,
            updated_at = updatedAt,
            updated_by = updatedBy,
            id = userId
        )
    }

    suspend fun countActiveAdmins(): Long = withContext(Dispatchers.Default) {
        queries.countActiveAdmins().executeAsOne()
    }

    suspend fun delete(id: String) = withContext(Dispatchers.Default) {
        queries.deleteUser(id)
    }

    /**
     * Upsert (INSERT OR REPLACE) a user.
     * Use this for sync operations to handle both new and existing records.
     */
    suspend fun upsert(user: User) = withContext(Dispatchers.Default) {
        queries.upsertUser(
            id = user.id,
            username = user.username,
            password = user.password,
            full_name = user.fullName,
            is_admin = if (user.isAdmin) 1L else 0L,
            is_active = if (user.isActive) 1L else 0L,
            created_at = user.createdAt,
            updated_at = user.updatedAt,
            created_by = user.createdBy,
            updated_by = user.updatedBy
        )
    }

    fun observeAll(): Flow<List<User>> {
        return queries.getAllUsers()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .map { list -> list.map { it.toModel() } }
    }

    private fun com.medistock.shared.db.App_users.toModel(): User {
        return User(
            id = id,
            username = username,
            password = password,
            fullName = full_name,
            isAdmin = is_admin != 0L,
            isActive = is_active != 0L,
            createdAt = created_at,
            updatedAt = updated_at,
            createdBy = created_by,
            updatedBy = updated_by
        )
    }
}
