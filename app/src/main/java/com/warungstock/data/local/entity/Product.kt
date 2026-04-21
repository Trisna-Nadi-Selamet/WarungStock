package com.warungstock.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val buyPrice: Long,
    val sellPrice: Long,             // harga jual grosir / normal
    val sellPriceRetail: Long = 0,   // harga jual eceran (BARU)
    val stock: Int,
    val satuan: String = "pcs",      // satuan stok (BARU)
    val category: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isLowStock: Boolean
        get() = stock <= LOW_STOCK_THRESHOLD

    companion object {
        const val LOW_STOCK_THRESHOLD = 5
    }
}
