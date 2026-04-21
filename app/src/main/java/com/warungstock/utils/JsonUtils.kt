package com.warungstock.utils

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.warungstock.data.local.entity.Product

object JsonUtils {
    private val gson = Gson()

    fun toJson(products: List<Product>): String {
        return gson.toJson(products)
    }

    fun fromJson(json: String): List<Product> {
        val type = object : TypeToken<List<Product>>() {}.type
        val imported: List<Product> = gson.fromJson(json, type)
        // Reset ID biar tidak collision, biarkan Room auto-generate
        return imported.map { it.copy(id = 0) }
    }
}