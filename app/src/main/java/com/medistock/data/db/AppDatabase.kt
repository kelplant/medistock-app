package com.medistock.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.medistock.data.dao.*
import com.medistock.data.entities.*

@Database(
    entities = [
        Product::class,
        Category::class,
        ProductPrice::class,
        ProductSale::class,
        StockMovement::class,
        Site::class,
        PurchaseBatch::class,
        Inventory::class,
        User::class,
        UserPermission::class,
        Sale::class,
        SaleItem::class,
        Customer::class,
        SaleBatchAllocation::class,
        PackagingType::class,
        AuditHistory::class,
        ProductTransfer::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productPriceDao(): ProductPriceDao
    abstract fun productSaleDao(): ProductSaleDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun siteDao(): SiteDao
    abstract fun purchaseBatchDao(): PurchaseBatchDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun userDao(): UserDao
    abstract fun userPermissionDao(): UserPermissionDao
    abstract fun saleDao(): SaleDao
    abstract fun saleItemDao(): SaleItemDao
    abstract fun customerDao(): CustomerDao
    abstract fun saleBatchAllocationDao(): SaleBatchAllocationDao
    abstract fun packagingTypeDao(): PackagingTypeDao
    abstract fun auditHistoryDao(): AuditHistoryDao
    abstract fun productTransferDao(): ProductTransferDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medistock-db"
                )
                .fallbackToDestructiveMigration() // Allow destructive migration for development
                .build().also { INSTANCE = it }
            }
        }
    }
}
