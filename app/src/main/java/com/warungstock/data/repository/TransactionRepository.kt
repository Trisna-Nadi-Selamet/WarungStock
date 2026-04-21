package com.warungstock.data.repository

import com.warungstock.data.local.dao.StockTransactionDao
import com.warungstock.data.local.entity.StockTransaction
import java.util.Calendar

class TransactionRepository(private val dao: StockTransactionDao) {

    suspend fun recordTransaction(transaction: StockTransaction) = dao.insert(transaction)

    // ─── HARIAN ──────────────────────────────────────────────────────────
    suspend fun getDailyTransactions(dateMillis: Long = System.currentTimeMillis()): List<StockTransaction> {
        val (start, end) = dayRange(dateMillis)
        return dao.getTransactionsForDay(start, end)
    }

    fun getDailyTransactionsLive(dateMillis: Long = System.currentTimeMillis()) =
        dao.getTransactionsForDayLive(*dayRange(dateMillis).toArray())

    // ─── BULANAN ─────────────────────────────────────────────────────────
    suspend fun getMonthlyTransactions(year: Int, month: Int): List<StockTransaction> {
        val (start, end) = monthRange(year, month)
        return dao.getTransactionsForMonth(start, end)
    }

    fun getMonthlyTransactionsLive(year: Int, month: Int) =
        dao.getTransactionsForMonthLive(*monthRange(year, month).toArray())

    // ─── SUMMARY ─────────────────────────────────────────────────────────
    suspend fun getDailySummary(dateMillis: Long = System.currentTimeMillis()): ReportSummary {
        val (start, end) = dayRange(dateMillis)
        return buildSummary(start, end)
    }

    suspend fun getMonthlySummary(year: Int, month: Int): ReportSummary {
        val (start, end) = monthRange(year, month)
        return buildSummary(start, end)
    }

    private suspend fun buildSummary(from: Long, to: Long): ReportSummary {
        val totalMasuk = dao.getTotalMasuk(from, to)
        val totalKeluar = dao.getTotalKeluar(from, to)
        val unitMasuk = dao.getTotalUnitMasuk(from, to)
        val unitKeluar = dao.getTotalUnitKeluar(from, to)
        val count = dao.getTransactionCount(from, to)
        return ReportSummary(
            totalNilaiMasuk = totalMasuk,
            totalNilaiKeluar = totalKeluar,
            totalUnitMasuk = unitMasuk,
            totalUnitKeluar = unitKeluar,
            totalTransaksi = count,
            estimasiLaba = totalKeluar - (totalMasuk) // simplified
        )
    }

    // ─── HELPERS ─────────────────────────────────────────────────────────
    private fun dayRange(millis: Long): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply { timeInMillis = millis }
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_MONTH, 1)
        return Pair(start, cal.timeInMillis)
    }

    private fun monthRange(year: Int, month: Int): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.MONTH, 1)
        return Pair(start, cal.timeInMillis)
    }

    data class ReportSummary(
        val totalNilaiMasuk: Long,
        val totalNilaiKeluar: Long,
        val totalUnitMasuk: Int,
        val totalUnitKeluar: Int,
        val totalTransaksi: Int,
        val estimasiLaba: Long
    )
}

private fun Pair<Long, Long>.toArray() = longArrayOf(first, second)
