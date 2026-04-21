package com.warungstock.utils

import com.warungstock.data.local.entity.StockTransaction
import com.warungstock.data.repository.TransactionRepository.ReportSummary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object ReportUtils {

    private val dateTimeFmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))
    private val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))
    private val monthFmt = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
    private val currency = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    // ─── CSV GENERATOR ────────────────────────────────────────────────────

    fun buildDailyCsv(
        dateMillis: Long,
        transactions: List<StockTransaction>,
        summary: ReportSummary
    ): String {
        val cal = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val header = "LAPORAN HARIAN WARUNGSTOCK"
        val dateStr = dateFmt.format(Date(dateMillis))
        return buildCsv(header, dateStr, "Harian", transactions, summary)
    }

    fun buildMonthlyCsv(
        year: Int,
        month: Int,
        transactions: List<StockTransaction>,
        summary: ReportSummary
    ): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
        }
        val monthStr = monthFmt.format(cal.time)
        return buildCsv("LAPORAN BULANAN WARUNGSTOCK", monthStr, "Bulanan", transactions, summary)
    }

    private fun buildCsv(
        title: String,
        periodLabel: String,
        reportType: String,
        transactions: List<StockTransaction>,
        summary: ReportSummary
    ): String {
        val sb = StringBuilder()

        // ── Info Header ──────────────────────────────────
        sb.appendLine("$title")
        sb.appendLine("Periode,\"$periodLabel\"")
        sb.appendLine("Digenerate Pada,\"${dateTimeFmt.format(Date())}\"")
        sb.appendLine("Tipe Laporan,\"$reportType\"")
        sb.appendLine()

        // ── Ringkasan Eksekutif ──────────────────────────
        sb.appendLine("=== RINGKASAN ===")
        sb.appendLine("Total Transaksi,${summary.totalTransaksi}")
        sb.appendLine()
        sb.appendLine("--- BARANG MASUK (RESTOCK) ---")
        sb.appendLine("Total Unit Masuk,${summary.totalUnitMasuk}")
        sb.appendLine("Total Nilai Masuk,\"${formatRupiah(summary.totalNilaiMasuk)}\"")
        sb.appendLine()
        sb.appendLine("--- BARANG KELUAR (PENJUALAN) ---")
        sb.appendLine("Total Unit Terjual,${summary.totalUnitKeluar}")
        sb.appendLine("Total Nilai Jual,\"${formatRupiah(summary.totalNilaiKeluar)}\"")
        sb.appendLine()
        sb.appendLine("--- ESTIMASI ---")
        sb.appendLine("Estimasi Laba Kotor,\"${formatRupiah(summary.estimasiLaba)}\"")
        sb.appendLine()

        // ── Detail Transaksi ─────────────────────────────
        sb.appendLine("=== DETAIL TRANSAKSI ===")
        sb.appendLine(
            "No,Waktu,Nama Barang,Kategori,Tipe,Jumlah,Satuan,Harga Satuan,Total Nilai,Catatan"
        )

        transactions.forEachIndexed { index, tx ->
            val tipe = if (tx.type == StockTransaction.TransactionType.MASUK) "MASUK (Restock)" else "KELUAR (Jual)"
            sb.appendLine(
                "${index + 1}," +
                "\"${dateTimeFmt.format(Date(tx.timestamp))}\"," +
                "\"${tx.productName}\"," +
                "\"${tx.category}\"," +
                "\"$tipe\"," +
                "${tx.quantity}," +
                "\"${tx.satuan}\"," +
                "\"${formatRupiah(tx.unitPrice)}\"," +
                "\"${formatRupiah(tx.totalValue)}\"," +
                "\"${tx.note}\""
            )
        }

        // ── Rekapitulasi per Barang ──────────────────────
        sb.appendLine()
        sb.appendLine("=== REKAPITULASI PER BARANG ===")
        sb.appendLine("Nama Barang,Kategori,Total Masuk (unit),Total Keluar (unit),Nilai Masuk,Nilai Keluar")

        val grouped = transactions.groupBy { it.productName }
        grouped.forEach { (name, txList) ->
            val masuk = txList.filter { it.type == StockTransaction.TransactionType.MASUK }
            val keluar = txList.filter { it.type == StockTransaction.TransactionType.KELUAR }
            val category = txList.first().category
            sb.appendLine(
                "\"$name\"," +
                "\"$category\"," +
                "${masuk.sumOf { it.quantity }}," +
                "${keluar.sumOf { it.quantity }}," +
                "\"${formatRupiah(masuk.sumOf { it.totalValue })}\"," +
                "\"${formatRupiah(keluar.sumOf { it.totalValue })}\""
            )
        }

        // ── Rekapitulasi per Kategori ────────────────────
        sb.appendLine()
        sb.appendLine("=== REKAPITULASI PER KATEGORI ===")
        sb.appendLine("Kategori,Transaksi Masuk,Transaksi Keluar,Nilai Masuk,Nilai Keluar")

        val byCategory = transactions.groupBy { it.category }
        byCategory.forEach { (cat, txList) ->
            val masuk = txList.filter { it.type == StockTransaction.TransactionType.MASUK }
            val keluar = txList.filter { it.type == StockTransaction.TransactionType.KELUAR }
            sb.appendLine(
                "\"$cat\"," +
                "${masuk.size}," +
                "${keluar.size}," +
                "\"${formatRupiah(masuk.sumOf { it.totalValue })}\"," +
                "\"${formatRupiah(keluar.sumOf { it.totalValue })}\""
            )
        }

        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine("Dibuat oleh WarungStock App")

        return sb.toString()
    }

    fun formatRupiah(amount: Long): String =
        currency.format(amount).replace(",00", "")

    // ─── FILENAME HELPERS ─────────────────────────────────────────────────

    fun dailyFileName(dateMillis: Long): String {
        val fmt = SimpleDateFormat("yyyyMMdd", Locale.US)
        return "laporan_harian_${fmt.format(Date(dateMillis))}.csv"
    }

    fun monthlyFileName(year: Int, month: Int): String {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, year); set(Calendar.MONTH, month)
        }
        val fmt = SimpleDateFormat("yyyyMM", Locale.US)
        return "laporan_bulanan_${fmt.format(cal.time)}.csv"
    }
}
