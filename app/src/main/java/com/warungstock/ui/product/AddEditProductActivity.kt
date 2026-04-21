package com.warungstock.ui.product

import android.app.Activity
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.warungstock.data.local.AppDatabase
import com.warungstock.data.local.entity.Product
import com.warungstock.databinding.ActivityAddEditProductBinding
import com.warungstock.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

class AddEditProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEditProductBinding
    private val viewModel: ProductViewModel by viewModels()
    private var editingProduct: Product? = null
    private var productId: Long = -1L

    companion object {
        const val EXTRA_PRODUCT_ID = "extra_product_id"
    }

    private val defaultCategories = listOf(
        "Makanan", "Minuman", "Snack", "Rokok", "Sembako",
        "Kebersihan", "Kesehatan", "Lainnya"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEditProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        productId = intent.getLongExtra(EXTRA_PRODUCT_ID, -1L)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (productId == -1L) "Tambah Barang" else "Edit Barang"

        setupCategoryDropdown()

        if (productId != -1L) {
            loadProduct()
        }

        binding.btnSave.setOnClickListener { saveProduct() }
    }

    private fun setupCategoryDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, defaultCategories)
        binding.actvCategory.setAdapter(adapter)
    }

    private fun loadProduct() {
        lifecycleScope.launch {
            val db = AppDatabase.getDatabase(applicationContext)
            val product = db.productDao().getProductById(productId)
            product?.let {
                editingProduct = it
                with(binding) {
                    etName.setText(it.name)
                    etBuyPrice.setText(it.buyPrice.toString())
                    etSellPrice.setText(it.sellPrice.toString())
                    etStock.setText(it.stock.toString())
                    actvCategory.setText(it.category, false)
                }
            }
        }
    }

    private fun saveProduct() {
        val name = binding.etName.text.toString().trim()
        val buyPriceStr = binding.etBuyPrice.text.toString().trim()
        val sellPriceStr = binding.etSellPrice.text.toString().trim()
        val stockStr = binding.etStock.text.toString().trim()
        val category = binding.actvCategory.text.toString().trim()

        // Validasi
        if (name.isEmpty()) {
            binding.tilName.error = "Nama barang tidak boleh kosong"
            return
        } else binding.tilName.error = null

        val buyPrice = buyPriceStr.toLongOrNull()
        if (buyPrice == null || buyPrice < 0) {
            binding.tilBuyPrice.error = "Harga beli tidak valid"
            return
        } else binding.tilBuyPrice.error = null

        val sellPrice = sellPriceStr.toLongOrNull()
        if (sellPrice == null || sellPrice < 0) {
            binding.tilSellPrice.error = "Harga jual tidak valid"
            return
        } else binding.tilSellPrice.error = null

        val stock = stockStr.toIntOrNull()
        if (stock == null || stock < 0) {
            binding.tilStock.error = "Stok tidak valid"
            return
        } else binding.tilStock.error = null

        if (category.isEmpty()) {
            binding.tilCategory.error = "Pilih kategori"
            return
        } else binding.tilCategory.error = null

        val product = if (editingProduct != null) {
            editingProduct!!.copy(
                name = name,
                buyPrice = buyPrice,
                sellPrice = sellPrice,
                stock = stock,
                category = category,
                updatedAt = System.currentTimeMillis()
            )
        } else {
            Product(
                name = name,
                buyPrice = buyPrice,
                sellPrice = sellPrice,
                stock = stock,
                category = category
            )
        }

        if (editingProduct != null) {
            viewModel.updateProduct(product)
        } else {
            viewModel.addProduct(product)
        }

        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}