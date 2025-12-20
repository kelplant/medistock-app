package com.medistock.data.repository

import android.content.Context
import com.medistock.data.db.AppDatabase
import com.medistock.data.entities.User
import com.medistock.util.AuditLogger
import com.medistock.util.AuthManager

/**
 * Repository for User operations with integrated audit logging
 */
class AuditedUserRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val userDao = database.userDao()
    private val auditLogger = AuditLogger.getInstance(context)
    private val authManager = AuthManager.getInstance(context)

    fun insert(user: User): Long {
        val userId = userDao.insert(user)

        // Log the insert action (don't log password)
        auditLogger.logInsert(
            entityType = "User",
            entityId = userId,
            newValues = mapOf(
                "username" to user.username,
                "fullName" to user.fullName,
                "isAdmin" to user.isAdmin.toString(),
                "isActive" to user.isActive.toString()
            ),
            username = authManager.getUsername(),
            siteId = null,
            description = "User created: ${user.username} (${user.fullName})"
        )

        return userId
    }

    fun update(oldUser: User, newUser: User) {
        userDao.update(newUser)

        // Track changes
        val changes = mutableMapOf<String, Pair<String?, String?>>()

        if (oldUser.username != newUser.username) {
            changes["username"] = Pair(oldUser.username, newUser.username)
        }
        if (oldUser.fullName != newUser.fullName) {
            changes["fullName"] = Pair(oldUser.fullName, newUser.fullName)
        }
        if (oldUser.isAdmin != newUser.isAdmin) {
            changes["isAdmin"] = Pair(oldUser.isAdmin.toString(), newUser.isAdmin.toString())
        }
        if (oldUser.isActive != newUser.isActive) {
            changes["isActive"] = Pair(oldUser.isActive.toString(), newUser.isActive.toString())
        }
        // Don't log password changes for security

        if (changes.isNotEmpty()) {
            auditLogger.logUpdate(
                entityType = "User",
                entityId = newUser.id,
                changes = changes,
                username = authManager.getUsername(),
                siteId = null,
                description = "User updated: ${newUser.username}"
            )
        }
    }

    fun delete(user: User) {
        userDao.delete(user)

        auditLogger.logDelete(
            entityType = "User",
            entityId = user.id,
            oldValues = mapOf(
                "username" to user.username,
                "fullName" to user.fullName,
                "isAdmin" to user.isAdmin.toString()
            ),
            username = authManager.getUsername(),
            siteId = null,
            description = "User deleted: ${user.username}"
        )
    }
}
