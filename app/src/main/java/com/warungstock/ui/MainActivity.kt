package com.warungstock.ui

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.RadioButton
import android.widget.TextView
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
import com.google.android.material.textfield.TextInputEditText
import com.warungstock.R
import com.warungstock.data.local.entity.Product
import com.warungstock.databinding.ActivityMainBinding
import com.warungstock.ui.product.AddEditProductActivity
import com.warungstock.ui.product.ProductAdapter
import com.warungstock.ui.settings.SettingsActivity // ✅ TAMBAHAN
import com.warungstock.utils.JsonUtils
import com.warungstock.utils.ReportUtils
import com.warungstock.viewmodel.ProductViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ProductViewModel by viewModels()
    private lateinit var adapter: ProductAdapter

    private val addProductLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                Toast.makeText(this, "Barang berhasil disimpan", Toast.LENGTH_SHORT).show()
            }
        }

    private val importLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
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

    // =========================
    // UI SETUP
    // =========================

    private fun setupRecyclerView() {
        adapter = ProductAdapter(
            onEdit = {
                val intent = Intent(this, AddEditProductActivity::class.java)
                intent.putExtra(AddEditProductActivity.EXTRA_PRODUCT_ID, it.id)
                addProductLauncher.launch(intent)
            },
            onDelete = {
                MaterialAlertDialogBuilder(this)
                    .setTitle("Hapus Barang")
                    .setMessage("Hapus ${it.name}?")
                    .setPositiveButton("Hapus") { _, _ ->
                        viewModel.deleteProduct(it)
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            },
            onAddStock = { showStockDialog(it, true) },
            onReduceStock = { showStockDialog(it, false) }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = this@MainActivity.adapter
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

    // =========================
    // OBSERVER
    // =========================

    private fun observeData() {

        viewModel.products.observe(this) {
            adapter.submitList(it)
            binding.tvEmpty.isVisible = it.isEmpty()
            binding.recyclerView.isVisible = it.isNotEmpty()
        }

        viewModel.allProducts.observe(this) { all ->

            binding.tvTotalBarang.text = all.size.toString()
            binding.tvStokRendah.text = all.count { it.stock in 1..5 }.toString()
            binding.tvStokHabis.text = all.count { it.stock <= 0 }.toString()

            val totalModal = all.fold(0L) { acc, p -> acc + (p.buyPrice * p.stock) }
            val totalJual = all.fold(0L) { acc, p -> acc + (p.sellPrice * p.stock) }

            binding.tvTotalModal.text = ReportUtils.formatRupiah(totalModal)
            binding.tvTotalNilaiJual.text = ReportUtils.formatRupiah(totalJual)
        }

        viewModel.categories.observe(this) {
            setupCategoryChips(it)
        }

        viewModel.lowStockProducts.observe(this) {
            binding.bannerLowStock.isVisible = it.isNotEmpty()
            if (it.isNotEmpty()) {
                binding.tvLowStockInfo.text = "${it.size} barang hampir habis"
            }
        }
    }

    private fun setupCategoryChips(categories: List<String>) {
        binding.chipGroupCategory.removeAllViews()

        val list = listOf("Semua") + categories

        list.forEach { category ->
            val chip = Chip(this).apply {
                text = category
                isCheckable = true
                isChecked = category == "Semua"
                setOnClickListener {
                    viewModel.setCategory(category)
                }
            }
            binding.chipGroupCategory.addView(chip)
        }
    }

    // =========================
    // DIALOG FIX (ANTI BUG)
    // =========================

    private fun showStockDialog(product: Product, isAdd: Boolean) {

        val view = layoutInflater.inflate(R.layout.dialog_stock_update, null)
        val etAmount = view.findViewById<TextInputEditText>(R.id.etAmount)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(if (isAdd) "Tambah Stok" else "Jual Barang")
            .setView(view)
            .setPositiveButton("Simpan", null)
            .setNegativeButton("Batal", null)
            .create()

        dialog.setOnShowListener {
            val btn = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)

            btn.setOnClickListener {
                val amount = etAmount.text.toString().toIntOrNull()

                if (amount == null || amount <= 0) {
                    etAmount.error = "Jumlah tidak valid"
                    return@setOnClickListener
                }

                if (isAdd) {
                    viewModel.addStock(product, amount)
                } else {
                    viewModel.reduceStock(product, amount)
                }

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    // =========================
    // MENU
    // =========================

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> { exportJson(); true }
            R.id.action_import -> { importLauncher.launch("application/json"); true }
            R.id.action_settings -> { // ✅ INI YANG NYAMBUNG KE SETTINGS
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // =========================
    // EXPORT / IMPORT FIX
    // =========================

    private fun exportJson() {
        lifecycleScope.launch {
            try {
                val products = viewModel.exportProducts()
                val json = JsonUtils.toJson(products)

                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, "warungstock.json")
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                }

                val uri = contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    values
                )

                uri?.let {
                    contentResolver.openOutputStream(it)?.use { stream ->
                        stream.write(json.toByteArray())
                    }
                    Toast.makeText(this@MainActivity, "Export berhasil", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Export gagal", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun importJson(uri: Uri) {
        try {
            val json = contentResolver.openInputStream(uri)?.bufferedReader()?.readText()
            val products = json?.let { JsonUtils.fromJson(it) } ?: emptyList()
            viewModel.importProducts(products)

            Toast.makeText(this, "Import berhasil", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "File tidak valid", Toast.LENGTH_SHORT).show()
        }
    }
}