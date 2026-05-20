package com.rudra.fintechvar.adapter

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.rudra.fintechvar.db.modal.CategoryEntity

class CategoryMultiSelectAdapter(
    private val categories: List<CategoryEntity>,
    selected: Set<Int>
) : RecyclerView.Adapter<CategoryMultiSelectAdapter.VH>() {

    private val selectedIds = selected.toMutableSet()

    fun getSelectedIds(): Set<Int> = selectedIds

    @SuppressLint("NotifyDataSetChanged")
    fun clearSelection() {
        selectedIds.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val cb = CheckBox(parent.context)
        cb.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return VH(cb)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val category = categories[position]
        holder.checkBox.text = category.categoryName
        holder.checkBox.isChecked = selectedIds.contains(category.categoryId)

        holder.checkBox.setOnCheckedChangeListener { _, checked ->
            if (checked) selectedIds.add(category.categoryId)
            else selectedIds.remove(category.categoryId)
        }
    }

    override fun getItemCount() = categories.size

    class VH(val checkBox: CheckBox) : RecyclerView.ViewHolder(checkBox)
}
