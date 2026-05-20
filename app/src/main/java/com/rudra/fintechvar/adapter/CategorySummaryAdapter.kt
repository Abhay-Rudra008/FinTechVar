package com.rudra.fintechvar.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rudra.fintechvar.databinding.ItemSummaryRowBinding
import com.rudra.fintechvar.db.modal.CategorySummary

class CategorySummaryAdapter :
    ListAdapter<CategorySummary, CategorySummaryAdapter.ViewHolder>(Diff()) {

    inner class ViewHolder(
        private val binding: ItemSummaryRowBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(item: CategorySummary) {
            binding.tvName.text = item.categoryName
            binding.tvAmount.text = "₹ %.2f".format(item.totalAmount)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSummaryRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class Diff : DiffUtil.ItemCallback<CategorySummary>() {
        override fun areItemsTheSame(old: CategorySummary, new: CategorySummary): Boolean =
            old.categoryName == new.categoryName

        override fun areContentsTheSame(old: CategorySummary, new: CategorySummary): Boolean =
            old == new
    }
}
