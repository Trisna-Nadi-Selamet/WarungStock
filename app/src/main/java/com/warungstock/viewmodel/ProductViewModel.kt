package com.warungstock.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.warungstock.data.local.AppDatabase
import com.warungstock.data.local.entity.Product
import com.warungstock.data.repository.ProductRepository
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    // ✅ FIX: repository pakai lazy biar tidak error initialization
    private val repository: ProductRepository by lazy {
        val db = AppDatabase.getDatabase(getApplication())
        ProductRepository(db.productDao())
    }

    private val _searchQuery = MutableLiveData("")
    private val _selectedCategory = MutableLiveData("Semua")

    private val _operationResult = MutableLiveData<Result<Unit>>()
    val operationResult: LiveData<Result<Unit>> = _operationResult

    // ✅ LiveData dari repository
    val allProducts: LiveData<List<Product>> by lazy {
        repository.getAllProducts()
    }

    val categories: LiveData<List<String>> by lazy {
        repository.getAllCategories()
    }

    val lowStockProducts: LiveData<List<Product>> by lazy {
        repository.getLowStockProducts()
    }

    // Filter trigger
    private val filterParams = MediatorLiveData<Pair<String, String>>().apply {
        addSource(_searchQuery) { q ->
            value = Pair(q ?: "", _selectedCategory.value ?: "Semua")
        }
        addSource(_selectedCategory) { c ->
            value = Pair(_searchQuery.value ?: "", c ?: "Semua")
        }
    }

    // Filtered products
    val products: LiveData<List<Product>> = filterParams.switchMap { (query, category) ->
        repository.searchAndFilter(query, category)
    }

    init {
        _searchQuery.value = ""
        _selectedCategory.value = "Semua"
    }

    // ======================
    // FILTER FUNCTIONS
    // ======================

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    // ======================
    // CRUD OPERATIONS
    // ======================

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

    // ======================
    // STOCK
    // ======================

    fun addStock(productId: Long, amount: Int) = viewModelScope.launch {
        repository.addStock(productId, amount)
    }

    fun reduceStock(productId: Long, amount: Int) = viewModelScope.launch {
        repository.reduceStock(productId, amount)
    }

    // ======================
    // IMPORT / EXPORT
    // ======================

    suspend fun exportProducts(): List<Product> {
        return repository.getAllProductsOnce()
    }

    fun importProducts(products: List<Product>) = viewModelScope.launch {
        repository.insertAll(products)
    }
}