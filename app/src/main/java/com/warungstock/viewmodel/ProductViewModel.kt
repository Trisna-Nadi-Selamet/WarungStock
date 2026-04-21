package com.warungstock.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.warungstock.data.local.AppDatabase
import com.warungstock.data.local.entity.Product
import com.warungstock.data.repository.ProductRepository
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProductRepository
    private val _searchQuery = MutableLiveData("")
    private val _selectedCategory = MutableLiveData("Semua")

    val allProducts: LiveData<List<Product>>
    val categories: LiveData<List<String>>
    val lowStockProducts: LiveData<List<Product>>

    // Combined filter result
    val filteredProducts: LiveData<List<Product>> = MediatorLiveData<List<Product>>().apply {
        var lastQuery = ""
        var lastCategory = "Semua"

        addSource(_searchQuery) { query ->
            lastQuery = query ?: ""
            value = null // trigger refresh via repository
        }
        addSource(_selectedCategory) { category ->
            lastCategory = category ?: "Semua"
        }
    }

    // We use switchMap for reactive filtering
    private val filterParams = MediatorLiveData<Pair<String, String>>().apply {
        addSource(_searchQuery) { q ->
            value = Pair(q ?: "", _selectedCategory.value ?: "Semua")
        }
        addSource(_selectedCategory) { c ->
            value = Pair(_searchQuery.value ?: "", c ?: "Semua")
        }
    }

    val products: LiveData<List<Product>> = filterParams.switchMap { (query, category) ->
        repository.searchAndFilter(query, category)
    }

    private val _operationResult = MutableLiveData<Result<Unit>>()
    val operationResult: LiveData<Result<Unit>> = _operationResult

    init {
        val db = AppDatabase.getDatabase(application)
        repository = ProductRepository(db.productDao())
        allProducts = repository.getAllProducts()
        categories = repository.getAllCategories()
        lowStockProducts = repository.getLowStockProducts()

        // Init filter params
        _searchQuery.value = ""
        _selectedCategory.value = "Semua"
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun addProduct(product: Product) = viewModelScope.launch {
        try {
            repository.insert(product)
            _operationResult.value = Result.success(Unit)
        } catch (e: Exception) {
            _operationResult.value = Result.failure(e)
        }
    }

    fun updateProduct(product: Product) = viewModelScope.launch {
        try {
            repository.update(product)
            _operationResult.value = Result.success(Unit)
        } catch (e: Exception) {
            _operationResult.value = Result.failure(e)
        }
    }

    fun deleteProduct(product: Product) = viewModelScope.launch {
        try {
            repository.delete(product)
            _operationResult.value = Result.success(Unit)
        } catch (e: Exception) {
            _operationResult.value = Result.failure(e)
        }
    }

    fun addStock(productId: Long, amount: Int) = viewModelScope.launch {
        repository.addStock(productId, amount)
    }

    fun reduceStock(productId: Long, amount: Int) = viewModelScope.launch {
        repository.reduceStock(productId, amount)
    }

    suspend fun exportProducts(): List<Product> = repository.getAllProductsOnce()

    fun importProducts(products: List<Product>) = viewModelScope.launch {
        repository.insertAll(products)
    }
}