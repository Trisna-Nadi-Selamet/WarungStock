package com.warungstock.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.warungstock.data.local.AppDatabase
import com.warungstock.data.local.entity.Product
import com.warungstock.data.local.entity.StockTransaction
import com.warungstock.data.repository.ProductRepository
import com.warungstock.data.repository.TransactionRepository
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {

    private val db by lazy { AppDatabase.getDatabase(getApplication()) }

    private val repository: ProductRepository by lazy {
        ProductRepository(db.productDao())
    }

    private val transactionRepo: TransactionRepository by lazy {
        TransactionRepository(db.stockTransactionDao())
    }

    private val _searchQuery = MutableLiveData("")
    private val _selectedCategory = MutableLiveData("Semua")

    private val _operationResult = MutableLiveData<Result<Unit>>()
    val operationResult: LiveData<Result<Unit>> = _operationResult

    val allProducts: LiveData<List<Product>> by lazy { repository.getAllProducts() }
    val categories: LiveData<List<String>> by lazy { repository.getAllCategories() }
    val lowStockProducts: LiveData<List<Product>> by lazy { repository.getLowStockProducts() }

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

    init {
        _searchQuery.value = ""
        _selectedCategory.value = "Semua"
    }

    // ─── FILTER ─────────────────────────────
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    // ─── CRUD ───────────────────────────────
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

    // ─── STOCK + TRANSACTION ─────────────────────────────

    fun addStock(product: Product, amount: Int, note: String = "") = viewModelScope.launch {
        repository.addStock(product.id, amount)

        transactionRepo.recordTransaction(
            StockTransaction(
                productId = product.id,
                productName = product.name,
                category = product.category,
                type = StockTransaction.TransactionType.MASUK,
                quantity = amount,
                unitPrice = product.buyPrice,
                totalValue = product.buyPrice * amount,
                note = note
            )
        )
    }

    fun reduceStock(product: Product, amount: Int, note: String = "") = viewModelScope.launch {
        repository.reduceStock(product.id, amount)

        transactionRepo.recordTransaction(
            StockTransaction(
                productId = product.id,
                productName = product.name,
                category = product.category,
                type = StockTransaction.TransactionType.KELUAR,
                quantity = amount,
                unitPrice = product.sellPrice,
                totalValue = product.sellPrice * amount,
                note = note
            )
        )
    }

    // ─── OVERLOAD (ID ONLY) ─────────────────────────────

    fun addStock(productId: Long, amount: Int) = viewModelScope.launch {
        repository.addStock(productId, amount)
    }

    fun reduceStock(productId: Long, amount: Int) = viewModelScope.launch {
        repository.reduceStock(productId, amount)
    }

    // ─── REPORT ─────────────────────────────

    data class ReportData(
        val transactions: List<StockTransaction>,
        val summary: TransactionRepository.ReportSummary
    )

    suspend fun getDailyReport(dateMillis: Long = System.currentTimeMillis()): ReportData {
        val transactions = transactionRepo.getDailyTransactions(dateMillis)
        val summary = transactionRepo.getDailySummary(dateMillis)
        return ReportData(transactions, summary)
    }

    suspend fun getMonthlyReport(year: Int, month: Int): ReportData {
        val transactions = transactionRepo.getMonthlyTransactions(year, month)
        val summary = transactionRepo.getMonthlySummary(year, month)
        return ReportData(transactions, summary)
    }

    // ─── IMPORT / EXPORT ─────────────────────────────

    suspend fun exportProducts(): List<Product> {
        return repository.getAllProductsOnce()
    }

    fun importProducts(products: List<Product>) = viewModelScope.launch {
        repository.insertAll(products)
    }
}