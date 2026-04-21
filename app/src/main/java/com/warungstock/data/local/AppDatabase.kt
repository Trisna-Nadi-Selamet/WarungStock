package com.warungstock.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.warungstock.data.local.dao.ProductDao
import com.warungstock.data.local.dao.StockTransactionDao
import com.warungstock.data.local.entity.Product
import com.warungstock.data.local.entity.StockTransaction

@Database(
    entities = [Product::class, StockTransaction::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun productDao(): ProductDao
    abstract fun stockTransactionDao(): StockTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // ─── MIGRATION 1 → 2: tambah field satuan, jualEceran, isiPerPak ───
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE products ADD COLUMN satuan TEXT NOT NULL DEFAULT 'pcs'")
                database.execSQL("ALTER TABLE products ADD COLUMN jualEceran INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE products ADD COLUMN isiPerPak INTEGER NOT NULL DEFAULT 1")
            }
        }

        // ─── MIGRATION 2 → 3: tambah tabel stock_transactions ────────────
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS stock_transactions (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        productId INTEGER NOT NULL,
                        productName TEXT NOT NULL,
                        category TEXT NOT NULL,
                        type TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        unitPrice INTEGER NOT NULL,
                        totalValue INTEGER NOT NULL,
                        satuan TEXT NOT NULL DEFAULT 'pcs',
                        note TEXT NOT NULL DEFAULT '',
                        timestamp INTEGER NOT NULL,
                        FOREIGN KEY(productId) REFERENCES products(id) ON DELETE CASCADE
                    )
                """.trimIndent())
                database.execSQL("CREATE INDEX IF NOT EXISTS index_stock_transactions_productId ON stock_transactions(productId)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_stock_transactions_timestamp ON stock_transactions(timestamp)")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "warungstock.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
