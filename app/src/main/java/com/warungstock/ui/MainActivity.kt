package com.warungstock.ui

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
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
import com.warungstock.utils.JsonUtils
import com.warungstock.utils.ReportUtils
import com.warungstock.viewmodel.ProductViewModel
import kotlinx.coroutines.launch
import java.util.Calendar

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
            onAddStock = { product -> showStockDialog(product, isAdd = true) },
            onReduceStock = { product -> showStockDialog(product, isAdd = false) }
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

        viewModel.allProducts.observe(this) { all ->
            // ─── Panel Ringkasan Inventaris ──────────────────────────
            binding.tvTotalBarang.text = all.size.toString()
            binding.tvStokRendah.text = all.count { it.isLowStock && !it.isOutOfStock }.toString()
            binding.tvStokHabis.text = all.count { it.isOutOfStock }.toString()
            val totalModal = all.sumOf { it.buyPrice * it.stock }
            val totalJual = all.sumOf { it.sellPrice * it.stock }
            binding.tvTotalModal.text = ReportUtils.formatRupiah(totalModal)
            binding.tvTotalNilaiJual.text = ReportUtils.formatRupiah(totalJual)
        }

        viewModel.categories.observe(this) { setupCategoryChips(it) }

        viewModel.lowStockProducts.observe(this) { lowStock ->
            binding.bannerLowStock.isVisible = lowStock.isNotEmpty()
            if (lowStock.isNotEmpty())
                binding.tvLowStockInfo.text = "${lowStock.size} barang hampir habis"
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
                    for (i in 0 until binding.chipGroupCategory.childCount) {
                        val c = binding.chipGroupCategory.getChildAt(i) as? Chip
                        c?.isChecked = c?.text == category
                    }
                }
            }
            binding.chipGroupCategory.addView(chip)
        }
    }

    // ─── STOCK DIALOG ─────────────────────────────────────────────────────

    private fun showStockDialog(product: Product, isAdd: Boolean) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_stock_update, null)
        val etAmount = dialogView.findViewById<TextInputEditText>(R.id.etAmount)
        val tvSatuanInfo = dialogView.findViewById<TextView>(R.id.tvSatuanInfo)
        val rgJualMode = dialogView.findViewById<android.widget.RadioGroup>(R.id.rgJualMode)
        val rbEceran = dialogView.findViewById<RadioButton>(R.id.rbEceran)
        val rbBungkus = dialogView.findViewById<RadioButton>(R.id.rbBungkus)
        val tvIsiPerPak = dialogView.findViewById<TextView>(R.id.tvIsiPerPak)

        tvSatuanInfo.text = "Satuan: ${product.satuan}"
        rbEceran.text = "Eceran (${product.satuan})"
        tvIsiPerPak.text = "1 pak = ${product.isiPerPak} ${product.satuan}"

        if (isAdd) {
            rgJualMode.visibility = View.GONE
            tvIsiPerPak.visibility = View.GONE
        } else {
            rgJualMode.setOnCheckedChangeListener { _, checkedId ->
                tvIsiPerPak.visibility =
                    if (checkedId == R.id.rbBungkus) View.VISIBLE else View.GONE
            }
        }

        val title = if (isAdd) "Tambah Stok" else "Jual Barang"
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Simpan") { _, _ ->
                val amount = etAmount.text.toString().toIntOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(this, "Jumlah tidak valid", Toast.LENGTH_SHORT).show()
                } else {
                    val finalAmount = if (!isAdd && rbBungkus.isChecked) {
                        amount * product.isiPerPak
                    } else amount

                    if (isAdd) viewModel.addStock(product, finalAmount)
                    else viewModel.reduceStock(product, finalAmount)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ─── MENU ─────────────────────────────────────────────────────────────

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_export -> { exportJson(); true }
            R.id.action_import -> { importLauncher.launch("application/json"); true }
            R.id.action_report_daily -> { showDailyReportDialog(); true }
            R.id.action_report_monthly -> { showMonthlyReportDialog(); true }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // ─── REPORT DIALOGS ───────────────────────────────────────────────────

    private fun showDailyReportDialog() {
        val today = System.currentTimeMillis()
        MaterialAlertDialogBuilder(this)
            .setTitle("📊 Download Laporan Harian")
            .setMessage("Unduh laporan transaksi hari ini?\n\nFormat: CSV\nIsi: Semua transaksi masuk/keluar, ringkasan, rekapitulasi per barang & kategori")
            .setPositiveButton("Download Hari Ini") { _, _ ->
                downloadDailyReport(today)
            }
            .setNeutralButton("Pilih Tanggal") { _, _ ->
                showDatePickerForReport()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showMonthlyReportDialog() {
        val now = Calendar.getInstance()
        val currentYear = now.get(Calendar.YEAR)
        val currentMonth = now.get(Calendar.MONTH)

        val months = arrayOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        val thisMonthLabel = "${months[currentMonth]} $currentYear"
        val lastMonthCal = Calendar.getInstance().apply { add(Calendar.MONTH, -1) }
        val lastMonthLabel = "${months[lastMonthCal.get(Calendar.MONTH)]} ${lastMonthCal.get(Calendar.YEAR)}"

        MaterialAlertDialogBuilder(this)
            .setTitle("📈 Download Laporan Bulanan")
            .setItems(
                arrayOf(
                    "Bulan ini — $thisMonthLabel",
                    "Bulan lalu — $lastMonthLabel",
                    "Pilih bulan lainnya..."
                )
            ) { _, which ->
                when (which) {
                    0 -> downloadMonthlyReport(currentYear, currentMonth)
                    1 -> downloadMonthlyReport(
                        lastMonthCal.get(Calendar.YEAR),
                        lastMonthCal.get(Calendar.MONTH)
                    )
                    2 -> showMonthYearPicker()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showDatePickerForReport() {
        val cal = Calendar.getInstance()
        android.app.DatePickerDialog(
            this,
            { _, year, month, day ->
                val selected = Calendar.getInstance().apply {
                    set(year, month, day)
                }.timeInMillis
                downloadDailyReport(selected)
            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showMonthYearPicker() {
        val months = arrayOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        val now = Calendar.getInstance()
        var selectedMonth = now.get(Calendar.MONTH)
        var selectedYear = now.get(Calendar.YEAR)

        val dialogView = layoutInflater.inflate(R.layout.dialog_month_year_picker, null)
        val npMonth = dialogView.findViewById<android.widget.NumberPicker>(R.id.npMonth)
        val npYear = dialogView.findViewById<android.widget.NumberPicker>(R.id.npYear)

        npMonth.displayedValues = months
        npMonth.minValue = 0
        npMonth.maxValue = 11
        npMonth.value = selectedMonth
        npMonth.setOnValueChangedListener { _, _, newVal -> selectedMonth = newVal }

        npYear.minValue = 2020
        npYear.maxValue = now.get(Calendar.YEAR)
        npYear.value = selectedYear
        npYear.setOnValueChangedListener { _, _, newVal -> selectedYear = newVal }

        MaterialAlertDialogBuilder(this)
            .setTitle("Pilih Bulan & Tahun")
            .setView(dialogView)
            .setPositiveButton("Download") { _, _ ->
                downloadMonthlyReport(selectedYear, selectedMonth)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // ─── DOWNLOAD FUNCTIONS ───────────────────────────────────────────────

    private fun downloadDailyReport(dateMillis: Long) {
        val loadingToast = Toast.makeText(this, "⏳ Membuat laporan...", Toast.LENGTH_LONG)
        loadingToast.show()

        lifecycleScope.launch {
            try {
                val reportData = viewModel.getDailyReport(dateMillis)
                val csv = ReportUtils.buildDailyCsv(dateMillis, reportData.transactions, reportData.summary)
                val fileName = ReportUtils.dailyFileName(dateMillis)
                saveCsvToDownloads(fileName, csv)
                loadingToast.cancel()
                showDownloadSuccess(fileName, reportData.transactions.size)
            } catch (e: Exception) {
                loadingToast.cancel()
                Toast.makeText(this@MainActivity, "❌ Gagal membuat laporan: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun downloadMonthlyReport(year: Int, month: Int) {
        val loadingToast = Toast.makeText(this, "⏳ Membuat laporan bulanan...", Toast.LENGTH_LONG)
        loadingToast.show()

        lifecycleScope.launch {
            try {
                val reportData = viewModel.getMonthlyReport(year, month)
                val csv = ReportUtils.buildMonthlyCsv(year, month, reportData.transactions, reportData.summary)
                val fileName = ReportUtils.monthlyFileName(year, month)
                saveCsvToDownloads(fileName, csv)
                loadingToast.cancel()
                showDownloadSuccess(fileName, reportData.transactions.size)
            } catch (e: Exception) {
                loadingToast.cancel()
                Toast.makeText(this@MainActivity, "❌ Gagal membuat laporan: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveCsvToDownloads(fileName: String, csv: String) {
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "text/csv")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = contentResolver.insert(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            contentValues
        ) ?: throw Exception("Tidak bisa membuat file")

        contentResolver.openOutputStream(uri)?.use { stream ->
            // Tambah BOM agar Excel baca UTF-8 dengan benar
            stream.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
            stream.write(csv.toByteArray(Charsets.UTF_8))
        }

        contentValues.clear()
        contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
        contentResolver.update(uri, contentValues, null, null)
    }

    private fun showDownloadSuccess(fileName: String, txCount: Int) {
        MaterialAlertDialogBuilder(this)
            .setTitle("✅ Laporan Berhasil Dibuat!")
            .setMessage(
                "File: $fileName\n" +
                "Total transaksi: $txCount\n\n" +
                "Tersimpan di folder Downloads.\n" +
                "Buka dengan Excel atau Google Sheets untuk tampilan terbaik."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    // ─── JSON EXPORT / IMPORT (existing) ─────────────────────────────────

    private fun exportJson() {
        lifecycleScope.launch {
            val products = viewModel.exportProducts()
            val json = JsonUtils.toJson(products)
            val fileName = "warungstock_${System.currentTimeMillis()}.json"
            try {
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/json")
                }
                val uri = contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues
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
