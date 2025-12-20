# Audit History System Documentation

## Overview

The Audit History System is a comprehensive tracking solution that logs all data changes and entries in the MediStock application. This system provides complete transparency and accountability for all operations performed in the system.

## Features

- **Complete Tracking**: All INSERT, UPDATE, and DELETE operations are logged
- **Field-Level Changes**: For updates, tracks old value → new value for each field
- **User Attribution**: Records which user performed each action
- **Timestamp Recording**: Every action is timestamped
- **Admin-Only Access**: Audit history is only viewable by administrators
- **Site Context**: Links actions to specific sites when applicable
- **No Suspend Functions**: Uses executor pattern for non-blocking, non-suspend operations

## Database Schema

### AuditHistory Entity

```kotlin
@Entity(tableName = "audit_history")
data class AuditHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,      // e.g., "Product", "Sale", "User"
    val entityId: Long,           // ID of the affected entity
    val actionType: String,       // "INSERT", "UPDATE", "DELETE"
    val fieldName: String?,       // Field that changed (null for INSERT/DELETE)
    val oldValue: String?,        // Previous value (null for INSERT)
    val newValue: String?,        // New value (null for DELETE)
    val changedBy: String,        // Username who made the change
    val changedAt: Long,          // Timestamp
    val siteId: Long?,            // Site context if applicable
    val description: String?      // Optional description
)
```

## Architecture

### Components

1. **AuditHistory Entity** - Database table definition
2. **AuditHistoryDao** - Data access object with query methods
3. **AuditLogger** - Utility class for logging audit entries
4. **Audited Repositories** - Repository pattern wrapping DAOs with audit logging
5. **AuditHistoryViewModel** - ViewModel for the audit history UI
6. **AuditHistoryAdapter** - RecyclerView adapter for displaying audit entries
7. **AuditHistoryActivity** - Admin UI for viewing audit history

### Data Flow

```
User Action → Audited Repository → DAO (Database Write) + AuditLogger → AuditHistory Table
```

## Usage Guide

### 1. Using AuditLogger Directly

For simple logging without a repository:

```kotlin
val auditLogger = AuditLogger.getInstance(context)
val authManager = AuthManager.getInstance(context)

// Log an INSERT
auditLogger.logInsert(
    entityType = "Product",
    entityId = productId,
    newValues = mapOf(
        "name" to "Aspirin",
        "price" to "10.50"
    ),
    username = authManager.getUsername(),
    siteId = siteId,
    description = "Product created"
)

// Log an UPDATE
auditLogger.logUpdate(
    entityType = "Product",
    entityId = productId,
    changes = mapOf(
        "price" to Pair("10.50", "12.00")  // old → new
    ),
    username = authManager.getUsername()
)

// Log a DELETE
auditLogger.logDelete(
    entityType = "Product",
    entityId = productId,
    oldValues = mapOf("name" to "Aspirin"),
    username = authManager.getUsername()
)
```

### 2. Using Audited Repositories

Repositories automatically log changes:

```kotlin
val productRepo = AuditedProductRepository(context)

// Insert with automatic audit logging
val productId = productRepo.insert(newProduct)

// Update with automatic change tracking
productRepo.update(oldProduct, newProduct)

// Delete with automatic logging
productRepo.delete(product)
```

### 3. Creating New Audited Repositories

To add audit logging to a new entity:

```kotlin
class AuditedYourEntityRepository(private val context: Context) {
    private val database = AppDatabase.getInstance(context)
    private val yourEntityDao = database.yourEntityDao()
    private val auditLogger = AuditLogger.getInstance(context)
    private val authManager = AuthManager.getInstance(context)

    fun insert(entity: YourEntity): Long {
        val entityId = yourEntityDao.insert(entity)

        auditLogger.logInsert(
            entityType = "YourEntity",
            entityId = entityId,
            newValues = mapOf(
                "field1" to entity.field1.toString(),
                "field2" to entity.field2.toString()
            ),
            username = authManager.getUsername()
        )

        return entityId
    }

    fun update(oldEntity: YourEntity, newEntity: YourEntity) {
        yourEntityDao.update(newEntity)

        val changes = mutableMapOf<String, Pair<String?, String?>>()

        if (oldEntity.field1 != newEntity.field1) {
            changes["field1"] = Pair(
                oldEntity.field1.toString(),
                newEntity.field1.toString()
            )
        }

        if (changes.isNotEmpty()) {
            auditLogger.logUpdate(
                entityType = "YourEntity",
                entityId = newEntity.id,
                changes = changes,
                username = authManager.getUsername()
            )
        }
    }

    fun delete(entity: YourEntity) {
        yourEntityDao.delete(entity)

        auditLogger.logDelete(
            entityType = "YourEntity",
            entityId = entity.id,
            oldValues = mapOf(
                "field1" to entity.field1.toString()
            ),
            username = authManager.getUsername()
        )
    }
}
```

## Existing Audited Repositories

The following repositories are already implemented with audit logging:

1. **AuditedProductRepository** - Product management
2. **AuditedSaleRepository** - Sales tracking
3. **AuditedUserRepository** - User management
4. **AuditedPurchaseBatchRepository** - Inventory purchases

## Integration Checklist

To integrate audit logging into existing code:

- [ ] Create an Audited Repository for the entity
- [ ] Replace direct DAO calls with repository calls in ViewModels
- [ ] Test INSERT operations are logged
- [ ] Test UPDATE operations track field changes
- [ ] Test DELETE operations are logged
- [ ] Verify user attribution is correct
- [ ] Verify site context is included when applicable

## Viewing Audit History

### Access Requirements

- User must be logged in
- User must have admin privileges (`isAdmin = true`)

### Navigation

1. Go to **Administration** from the home screen
2. Click **Audit History** button (visible only to admins)
3. View all audit entries sorted by most recent first

### Audit Entry Display

Each entry shows:
- **Entity Type and ID** (e.g., "Product #123")
- **Action Type** (INSERT/UPDATE/DELETE) with color coding:
  - INSERT: Green
  - UPDATE: Orange
  - DELETE: Red
- **Change Details**:
  - INSERT: All new values
  - UPDATE: Field name with old → new values
  - DELETE: All deleted values
- **User and Timestamp**: Who made the change and when

## Querying Audit History

The `AuditHistoryDao` provides several query methods:

```kotlin
// Get all entries
auditHistoryDao.getAll()

// Get by entity type
auditHistoryDao.getByEntityType("Product")

// Get by specific entity
auditHistoryDao.getByEntity("Product", productId)

// Get by user
auditHistoryDao.getByUser("john_doe")

// Get by site
auditHistoryDao.getBySite(siteId)

// Get by date range
auditHistoryDao.getByDateRange(startTime, endTime)

// Advanced filtering
auditHistoryDao.getFiltered(
    entityType = "Sale",
    actionType = "INSERT",
    username = null,
    siteId = siteId,
    startTime = startDate,
    endTime = endDate,
    limit = 100,
    offset = 0
)
```

## Performance Considerations

- **Asynchronous Logging**: Uses a single-threaded executor to avoid blocking UI
- **No Suspend Functions**: Compatible with all existing code without coroutine requirements
- **Indexed Queries**: Database indexes on frequently queried fields
- **Batch Operations**: Consider batch inserts for bulk operations

## Maintenance

### Cleanup Old Entries

To prevent unlimited growth of audit history:

```kotlin
val auditLogger = AuditLogger.getInstance(context)
auditLogger.cleanupOldEntries(daysToKeep = 90) // Keep last 90 days
```

### Database Version

The audit history system was added in database version 8. The migration uses `fallbackToDestructiveMigration()` during development.

## Security Considerations

1. **Sensitive Data**: Password changes are NOT logged in User audit entries
2. **Admin Only**: Only administrators can view audit history
3. **Immutable Records**: Audit entries cannot be edited or deleted (except by cleanup)
4. **User Attribution**: Always captures the username from AuthManager

## Troubleshooting

### Audit entries not appearing

- Check that you're using an Audited Repository, not calling DAO directly
- Verify user is logged in (AuthManager.getUsername() returns valid username)
- Check database version is 8 or higher

### "Access denied" when viewing history

- User must have `isAdmin = true` in the User entity
- Check AuthManager.isAdmin() returns true

### Executor-related issues

- If you see threading errors, ensure you're not calling blocking DAO methods on the main thread
- The AuditLogger handles its own threading internally

## Future Enhancements

Potential improvements to consider:

- Export audit history to CSV/Excel
- Advanced filtering in the UI (date range, entity type, etc.)
- Audit history for specific entity (e.g., view all changes to Product #123)
- Real-time notifications for critical changes
- Audit history dashboard with statistics
- Search functionality within audit entries
- Pagination for large audit logs

## Support

For questions or issues with the audit history system:
1. Check this documentation
2. Review the example repositories
3. Examine the AuditLogger implementation
4. Test with a simple entity first

## Complete File List

### Core System Files
- `app/src/main/java/com/medistock/data/entities/AuditHistory.kt`
- `app/src/main/java/com/medistock/data/dao/AuditHistoryDao.kt`
- `app/src/main/java/com/medistock/util/AuditLogger.kt`

### Repository Files
- `app/src/main/java/com/medistock/data/repository/AuditedProductRepository.kt`
- `app/src/main/java/com/medistock/data/repository/AuditedSaleRepository.kt`
- `app/src/main/java/com/medistock/data/repository/AuditedUserRepository.kt`
- `app/src/main/java/com/medistock/data/repository/AuditedPurchaseBatchRepository.kt`

### UI Files
- `app/src/main/java/com/medistock/ui/viewmodel/AuditHistoryViewModel.kt`
- `app/src/main/java/com/medistock/ui/adapters/AuditHistoryAdapter.kt`
- `app/src/main/java/com/medistock/ui/admin/AuditHistoryActivity.kt`
- `app/src/main/res/layout/activity_audit_history.xml`
- `app/src/main/res/layout/item_audit_history.xml`

### Modified Files
- `app/src/main/java/com/medistock/data/db/AppDatabase.kt` (version 8, added AuditHistory entity)
- `app/src/main/java/com/medistock/ui/admin/AdminActivity.kt` (added Audit History button)
- `app/src/main/res/layout/activity_admin.xml` (added button UI)
- `app/src/main/AndroidManifest.xml` (registered AuditHistoryActivity)
