package com.warungstock.data.local

import androidx.room.TypeConverter
import com.warungstock.data.local.entity.StockTransaction

class Converters {

    @TypeConverter
    fun fromTransactionType(type: StockTransaction.TransactionType): String = type.name

    @TypeConverter
    fun toTransactionType(value: String): StockTransaction.TransactionType =
        StockTransaction.TransactionType.valueOf(value)
}
