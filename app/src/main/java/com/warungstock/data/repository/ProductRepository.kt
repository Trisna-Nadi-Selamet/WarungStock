package com.warungstock.data.repository

import androidx.lifecycle.LiveData
import com.warungstock.data.local.dao.ProductDao
import com.warungstock.data.local.entity.Product

class ProductRepository(private val dao: ProductDao) {

    fun getAllProducts(): LiveData<List<Product>> = dao.getAllProducts()

    fun searchAndFilter(query: String, category: String): LiveData<List<Product>> =
        dao.searchAndFilter(query, category)

    fun getAllCategories(): LiveData<List<String>> = dao.getAllCategories()

    fun getLowStockProducts(): LiveData<List<Product>> = dao.getLowStockProducts()

    suspend fun insert(product: Product): Long = dao.insert(product)

    suspend fun update(product: Product) = dao.update(product)

    suspend fun delete(product: Product) = dao.delete(product)

    suspend fun addStock(id: Long, amount: Int) = dao.addStock(id, amount)

    suspend fun reduceStock(id: Long, amount: Int) = dao.reduceStock(id, amount)

    suspend fun getAllProductsOnce(): List<Product> = dao.getAllProductsOnce()

    suspend fun insertAll(products: List<Product>) = dao.insertAll(products)
}