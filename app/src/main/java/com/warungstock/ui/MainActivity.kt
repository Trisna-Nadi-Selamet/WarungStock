package com.warungstock.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.warungstock.R
import com.warungstock.databinding.ActivityMainBinding
import com.warungstock.ui.product.AddEditProductActivity
import com.warungstock.ui.product.ProductAdapter
import com.warungstock.utils.JsonUtils
import com.warungstock.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ProductViewModel by viewModels()
    private lateinit var adapter: ProductAdapter

    private val addProductLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Barang berhasil disimpan", Toast.LENGTH_SHORT).show()
        }
    }

    private val importLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { importJson(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        setupRecyclerView()
        setupSearch()
        setupFab()
        observeData()
    }

    private fun setupRecyclerView() {
        adapter = ProductAdapter(
            onEdit = { product ->
                val intent = Intent(this, AddEditProductActivity::class.java)
                intent.putExtra(AddEditProductActivity.EXTRA_PRODUCT_ID, product.id)
                addProductLauncher.launch(intent)
            },
            onDelete = { product ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("Hapus Barang")
                    .setMessage("Hapus ${product.name}?")
                    .setPositiveButton("Hapus") { _, _ ->
                        viewModel.deleteProduct(product)
                        Toast.makeText(this, "Barang dihapus", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            },
            onAddStock = { product ->
                showStockDialog(product.id, isAdd = true)
            },
            onReduceStock = { product ->
                showStockDialog(product.id, isAdd = false)
            }
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false
            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })
    }

    private fun setupFab() {
        binding.fabAdd.setOnClickListener {
            addProductLauncher.launch(Intent(this, AddEditProductActivity::class.java))
        }
    }

    private fun observeData() {
        viewModel.products.observe(this) { products ->
            adapter.submitList(products)
            binding.tvEmpty.isVisible = products.isEmpty()
            binding.recyclerView.isVisible = products.isNotEmpty()
        }

        viewModel.categories.observe(this) { categories ->
            setupCategoryChips(categories)
        }

        viewModel.lowStockProducts.observe(this) { lowStock ->
            if (lowStock.isNotEmpty()) {
                binding.bannerLowStock.isVisible = true
                binding.tvLowStockInfo.text = "${lowStock.size} barang hampir habis"
            } else {
                binding.bannerLowStock.isVisible = false
            }
        }
    }

    private fun setupCategoryChips(categories: List<String>) {
        binding.chipGroupCategory.removeAllViews()

        val allCategories = listOf("Semua") + categories
        allCategories.forEach { category ->
            val chip = Chip(this).apply {
                text = category
                isCheckable = true
                isChecked = category == "Semua"
                setOnClickListener {
                    viewModel.setCategory(category)
                    // Uncheck other chips
                    for (i in 0 until binding.chipGroupCategory.childCount) {
                        val c = binding.chipGroupCategory.getChildAt(i) as? Chip
                        c?.isChecked = c?.text == category
                    }
                }
            }
            binding.chipGroupCategory.addView(chip)
        }
    }

    private fun showStockDialog(productId: Long, isAdd: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_stock_update, null)
        val etAmount = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etAmount)
        val title = if (isAdd) "Tambah Stok" else "Kurangi Stok (Jual)"

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val amount = etAmount.text.toString().toIntOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(this, "Jumlah tidak valid", Toast.LENGTH_SHORT).show()
                } else {
                    if (isAdd) viewModel.addStock(productId, amount)
                    else viewModel.reduceStock(productId, amount)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> {
                exportJson()
                true
            }
            R.id.action_import -> {
                importLauncher.launch("application/json")
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun exportJson() {
        lifecycleScope.launch {
            val products = viewModel.exportProducts()
            val json = JsonUtils.toJson(products)
            val fileName = "warungstock_${System.currentTimeMillis()}.json"

            try {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.Downloads.MIME_TYPE, "application/json")
                }
                val uri = contentResolver.insert(
                    android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(json.toByteArray())
                    }
                    Toast.makeText(this@MainActivity, "Export berhasil: $fileName", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Export gagal: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importJson(uri: Uri) {
        try {
            val json = contentResolver.openInputStream(uri)?.bufferedReader()?.readText() ?: return
            val products = JsonUtils.fromJson(json)
            viewModel.importProducts(products)
            Toast.makeText(this, "Import ${products.size} barang berhasil", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Import gagal: Format JSON tidak valid", Toast.LENGTH_SHORT).show()
        }
    }
}