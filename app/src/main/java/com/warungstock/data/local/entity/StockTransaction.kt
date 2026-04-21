package com.warungstock.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "stock_transactions",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("productId"), Index("timestamp")]
)
data class StockTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,      // snapshot nama barang saat transaksi
    val category: String,         // snapshot kategori
    val type: TransactionType,    // MASUK / KELUAR
    val quantity: Int,            // jumlah unit
    val unitPrice: Long,          // harga per unit saat transaksi (grosir/beli)
    val retailPrice: Long = 0,    // harga jual eceran saat transaksi (BARU)
    val totalValue: Long,         // quantity * unitPrice
    val satuan: String = "pcs",   // satuan unit
    val note: String = "",        // catatan opsional
    val timestamp: Long = System.currentTimeMillis()
) {
    enum class TransactionType {
        MASUK,   // tambah stok / restock
        KELUAR   // jual / kurangi stok
    }
}
