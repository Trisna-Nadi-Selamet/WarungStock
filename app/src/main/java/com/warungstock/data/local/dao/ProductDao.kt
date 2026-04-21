package com.warungstock.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.warungstock.data.local.entity.Product

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): LiveData<List<Product>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchByName(query: String): LiveData<List<Product>>

    @Query("SELECT * FROM products WHERE category = :category ORDER BY name ASC")
    fun filterByCategory(category: String): LiveData<List<Product>>

    @Query("""
        SELECT * FROM products 
        WHERE (:query = '' OR name LIKE '%' || :query || '%')
        AND (:category = 'Semua' OR category = :category)
        ORDER BY name ASC
    """)
    fun searchAndFilter(query: String, category: String): LiveData<List<Product>>

    @Query("SELECT DISTINCT category FROM products ORDER BY category ASC")
    fun getAllCategories(): LiveData<List<String>>

    @Query("SELECT * FROM products WHERE stock <= :threshold ORDER BY stock ASC")
    fun getLowStockProducts(threshold: Int = 5): LiveData<List<Product>>

    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: Long): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)

    @Query("UPDATE products SET stock = stock + :amount, updatedAt = :timestamp WHERE id = :id")
    suspend fun addStock(id: Long, amount: Int, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE products SET stock = MAX(0, stock - :amount), updatedAt = :timestamp WHERE id = :id")
    suspend fun reduceStock(id: Long, amount: Int, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM products")
    suspend fun getAllProductsOnce(): List<Product>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(products: List<Product>)
}