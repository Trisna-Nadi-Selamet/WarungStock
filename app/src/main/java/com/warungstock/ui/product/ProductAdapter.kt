package com.warungstock.ui.product

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.warungstock.R
import com.warungstock.data.local.entity.Product
import com.warungstock.databinding.ItemProductBinding
import java.text.NumberFormat
import java.util.Locale

class ProductAdapter(
    private val onEdit: (Product) -> Unit,
    private val onDelete: (Product) -> Unit,
    private val onAddStock: (Product) -> Unit,
    private val onReduceStock: (Product) -> Unit
) : ListAdapter<Product, ProductAdapter.ViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemProductBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) = with(binding) {
            tvName.text = product.name
            tvCategory.text = product.category
            tvBuyPrice.text = "Beli: ${product.buyPrice.toRupiah()}"
            tvSellPrice.text = "Jual: ${product.sellPrice.toRupiah()}"
            tvStock.text = "Stok: ${product.stock}"

            // Low stock warning
            if (product.isLowStock) {
                tvStock.setTextColor(ContextCompat.getColor(root.context, R.color.color_low_stock))
                ivWarning.visibility = android.view.View.VISIBLE
            } else {
                tvStock.setTextColor(ContextCompat.getColor(root.context, android.R.color.darker_gray))
                ivWarning.visibility = android.view.View.GONE
            }

            btnEdit.setOnClickListener { onEdit(product) }
            btnDelete.setOnClickListener { onDelete(product) }
            btnAddStock.setOnClickListener { onAddStock(product) }
            btnReduceStock.setOnClickListener { onReduceStock(product) }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(oldItem: Product, newItem: Product) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Product, newItem: Product) =
                oldItem == newItem
        }
    }
}

fun Long.toRupiah(): String {
    val format = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    return format.format(this).replace(",00", "")
}