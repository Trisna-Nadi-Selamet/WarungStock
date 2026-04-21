package com.warungstock.utils

import com.warungstock.data.local.entity.StockTransaction
import com.warungstock.data.repository.TransactionRepository.ReportSummary
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

object ReportUtils {

    private val dateTimeFmt = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale("id", "ID"))
    private val dateFmt     = SimpleDateFormat("dd/MM/yyyy",          Locale("id", "ID"))
    private val monthFmt    = SimpleDateFormat("MMMM yyyy",           Locale("id", "ID"))
    private val currency    = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    // ─── CSV GENERATOR ────────────────────────────────────────────────────

    fun buildDailyCsv(
        dateMillis: Long,
        transactions: List<StockTransaction>,
        summary: ReportSummary
    ): String {
        val dateStr = dateFmt.format(Date(dateMillis))
        return buildCsv("LAPORAN HARIAN WARUNGSTOCK", dateStr, "Harian", transactions, summary)
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
        sb.appendLine(title)
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
            "No,Waktu,Nama Barang,Kategori,Tipe,Jumlah,Satuan," +
            "Harga Satuan,Harga Eceran,Total Nilai,Catatan"  // <-- kolom Harga Eceran ditambahkan
        )

        transactions.forEachIndexed { index, tx ->
            val tipe = if (tx.type == StockTransaction.TransactionType.MASUK)
                           "MASUK (Restock)" else "KELUAR (Jual)"

            // Harga eceran hanya relevan untuk transaksi KELUAR
            val hargaEceran = if (tx.type == StockTransaction.TransactionType.KELUAR &&
                                  tx.retailPrice > 0)
                                  formatRupiah(tx.retailPrice)
                              else "-"

            sb.appendLine(
                "${index + 1}," +
                "\"${dateTimeFmt.format(Date(tx.timestamp))}\"," +
                "\"${tx.productName}\"," +
                "\"${tx.category}\"," +
                "\"$tipe\"," +
                "${tx.quantity}," +
                "\"${tx.satuan}\"," +
                "\"${formatRupiah(tx.unitPrice)}\"," +
                "\"$hargaEceran\"," +
                "\"${formatRupiah(tx.totalValue)}\"," +
                "\"${tx.note}\""
            )
        }

        // ── Rekapitulasi per Barang ──────────────────────
        sb.appendLine()
        sb.appendLine("=== REKAPITULASI PER BARANG ===")
        sb.appendLine("Nama Barang,Kategori,Satuan,Total Masuk (unit),Total Keluar (unit),Nilai Masuk,Nilai Keluar,Est. Laba")

        val grouped = transactions.groupBy { it.productName }
        grouped.forEach { (name, txList) ->
            val masuk  = txList.filter { it.type == StockTransaction.TransactionType.MASUK }
            val keluar = txList.filter { it.type == StockTransaction.TransactionType.KELUAR }
            val category = txList.first().category
            val satuan   = txList.first().satuan
            val nilaiMasuk  = masuk.sumOf { it.totalValue }
            val nilaiKeluar = keluar.sumOf { it.totalValue }
            // Estimasi laba per barang = nilai keluar - nilai masuk (proporsional)
            val unitMasuk  = masuk.sumOf { it.quantity }
            val unitKeluar = keluar.sumOf { it.quantity }
            val avgBuy     = if (unitMasuk > 0) nilaiMasuk / unitMasuk else 0L
            val estLaba    = nilaiKeluar - (avgBuy * unitKeluar)

            sb.appendLine(
                "\"$name\"," +
                "\"$category\"," +
                "\"$satuan\"," +
                "$unitMasuk," +
                "$unitKeluar," +
                "\"${formatRupiah(nilaiMasuk)}\"," +
                "\"${formatRupiah(nilaiKeluar)}\"," +
                "\"${formatRupiah(estLaba)}\""
            )
        }

        // ── Rekapitulasi per Kategori ────────────────────
        sb.appendLine()
        sb.appendLine("=== REKAPITULASI PER KATEGORI ===")
        sb.appendLine("Kategori,Transaksi Masuk,Transaksi Keluar,Nilai Masuk,Nilai Keluar,Est. Laba")

        val byCategory = transactions.groupBy { it.category }
        byCategory.forEach { (cat, txList) ->
            val masuk  = txList.filter { it.type == StockTransaction.TransactionType.MASUK }
            val keluar = txList.filter { it.type == StockTransaction.TransactionType.KELUAR }
            val nilaiMasuk  = masuk.sumOf { it.totalValue }
            val nilaiKeluar = keluar.sumOf { it.totalValue }
            val unitMasuk   = masuk.sumOf { it.quantity }
            val unitKeluar  = keluar.sumOf { it.quantity }
            val avgBuy      = if (unitMasuk > 0) nilaiMasuk / unitMasuk else 0L
            val estLaba     = nilaiKeluar - (avgBuy * unitKeluar)

            sb.appendLine(
                "\"$cat\"," +
                "${masuk.size}," +
                "${keluar.size}," +
                "\"${formatRupiah(nilaiMasuk)}\"," +
                "\"${formatRupiah(nilaiKeluar)}\"," +
                "\"${formatRupiah(estLaba)}\""
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
