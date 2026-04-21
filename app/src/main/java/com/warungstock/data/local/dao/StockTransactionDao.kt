package com.warungstock.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.warungstock.data.local.entity.StockTransaction

@Dao
interface StockTransactionDao {

    @Insert
    suspend fun insert(transaction: StockTransaction): Long

    // ─── QUERY HARIAN ───────────────────────────────────────────────────
    @Query("""
        SELECT * FROM stock_transactions
        WHERE timestamp >= :startOfDay AND timestamp < :endOfDay
        ORDER BY timestamp DESC
    """)
    suspend fun getTransactionsForDay(startOfDay: Long, endOfDay: Long): List<StockTransaction>

    @Query("""
        SELECT * FROM stock_transactions
        WHERE timestamp >= :startOfDay AND timestamp < :endOfDay
        ORDER BY timestamp DESC
    """)
    fun getTransactionsForDayLive(startOfDay: Long, endOfDay: Long): LiveData<List<StockTransaction>>

    // ─── QUERY BULANAN ──────────────────────────────────────────────────
    @Query("""
        SELECT * FROM stock_transactions
        WHERE timestamp >= :startOfMonth AND timestamp < :endOfMonth
        ORDER BY timestamp DESC
    """)
    suspend fun getTransactionsForMonth(startOfMonth: Long, endOfMonth: Long): List<StockTransaction>

    @Query("""
        SELECT * FROM stock_transactions
        WHERE timestamp >= :startOfMonth AND timestamp < :endOfMonth
        ORDER BY timestamp DESC
    """)
    fun getTransactionsForMonthLive(startOfMonth: Long, endOfMonth: Long): LiveData<List<StockTransaction>>

    // ─── SUMMARY ────────────────────────────────────────────────────────
    @Query("""
        SELECT COALESCE(SUM(totalValue), 0) FROM stock_transactions
        WHERE type = 'MASUK' AND timestamp >= :from AND timestamp < :to
    """)
    suspend fun getTotalMasuk(from: Long, to: Long): Long

    @Query("""
        SELECT COALESCE(SUM(totalValue), 0) FROM stock_transactions
        WHERE type = 'KELUAR' AND timestamp >= :from AND timestamp < :to
    """)
    suspend fun getTotalKeluar(from: Long, to: Long): Long

    @Query("""
        SELECT COALESCE(SUM(quantity), 0) FROM stock_transactions
        WHERE type = 'MASUK' AND timestamp >= :from AND timestamp < :to
    """)
    suspend fun getTotalUnitMasuk(from: Long, to: Long): Int

    @Query("""
        SELECT COALESCE(SUM(quantity), 0) FROM stock_transactions
        WHERE type = 'KELUAR' AND timestamp >= :from AND timestamp < :to
    """)
    suspend fun getTotalUnitKeluar(from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM stock_transactions WHERE timestamp >= :from AND timestamp < :to")
    suspend fun getTransactionCount(from: Long, to: Long): Int

    @Query("""
        SELECT * FROM stock_transactions
        WHERE productId = :productId
        ORDER BY timestamp DESC LIMIT :limit
    """)
    fun getTransactionsForProduct(productId: Long, limit: Int = 20): LiveData<List<StockTransaction>>

    @Query("DELETE FROM stock_transactions WHERE timestamp < :before")
    suspend fun deleteOlderThan(before: Long)
}
