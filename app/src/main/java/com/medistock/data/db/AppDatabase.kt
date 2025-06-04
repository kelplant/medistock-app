package com.medistock.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.medistock.data.dao.*
import com.medistock.data.entities.*

@Database(
    entities = [
        Product::class,
        Category::class,
        ProductPrice::class,
        StockMovement::class,
        ProductSale::class,
        Site::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun categoryDao(): CategoryDao
    abstract fun productPriceDao(): ProductPriceDao
    abstract fun stockMovementDao(): StockMovementDao
    abstract fun productSaleDao(): ProductSaleDao
    abstract fun siteDao(): SiteDao
}