package com.warungstock.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val buyPrice: Long,
    val sellPrice: Long,
    val stock: Int,
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