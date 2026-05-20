package com.rudra.fintechvar.adapter

import android.graphics.Color
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rudra.fintechvar.db.modal.CategoryEntity
import java.io.File
import com.rudra.fintechvar.R
import com.rudra.fintechvar.databinding.ItemCategorySampleBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rudra.fintechvar.databinding.ItemCategoryAddBinding
import android.view.View
import com.google.android.material.chip.Chip

sealed class CategoryUiItem {
    data class CategoryItem(val entity: CategoryEntity) : CategoryUiItem()
    object AddButtonItem : CategoryUiItem()
}

class CategoryAdapter(
    private val onAddCategory: () -> Unit,
    private val onEditCategory: (CategoryEntity) -> Unit,
    private val onDeleteCategory: (CategoryEntity) -> Unit,
    private val onAddKeyword: (CategoryEntity) -> Unit,
    private val onDeleteKeyword: (String, CategoryEntity) -> Unit
) : ListAdapter<CategoryUiItem, RecyclerView.ViewHolder>(DIFF) {

    private var synonymsMap: Map<String, List<String>> = emptyMap()

    private var expandedCategoryId: Int? = null

    fun updateSynonyms(map: Map<String, List<String>>) {
        synonymsMap = map
        notifyDataSetChanged()
    }

    fun isItemExpanded(position: Int): Boolean {
        if (position < 0 || position >= itemCount) return false
        val item = getItem(position)
        return item is CategoryUiItem.CategoryItem && item.entity.categoryId == expandedCategoryId
    }

    companion object {
        const val TYPE_CATEGORY = 0
        const val TYPE_ADD_BUTTON = 1

        val DIFF = object : DiffUtil.ItemCallback<CategoryUiItem>() {
            override fun areItemsTheSame(a: CategoryUiItem, b: CategoryUiItem): Boolean {
                return when {
                    a is CategoryUiItem.CategoryItem && b is CategoryUiItem.CategoryItem -> a.entity.categoryId == b.entity.categoryId
                    a is CategoryUiItem.AddButtonItem && b is CategoryUiItem.AddButtonItem -> true
                    else -> false
                }
            }

            override fun areContentsTheSame(a: CategoryUiItem, b: CategoryUiItem) = a == b
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is CategoryUiItem.CategoryItem -> TYPE_CATEGORY
            is CategoryUiItem.AddButtonItem -> TYPE_ADD_BUTTON
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_ADD_BUTTON -> {
                val binding = ItemCategoryAddBinding.inflate(inflater, parent, false)
                AddViewHolder(binding, onAddCategory)
            }

            else -> {
                val binding = ItemCategorySampleBinding.inflate(inflater, parent, false)
                CategoryViewHolder(
                    binding, onEditCategory, onDeleteCategory, onAddKeyword, onDeleteKeyword
                )
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is CategoryUiItem.CategoryItem -> {
                val keywords = synonymsMap[item.entity.categoryName] ?: emptyList()
                val isExpanded = (expandedCategoryId == item.entity.categoryId)

                (holder as CategoryViewHolder).bind(
                    item.entity, keywords, isExpanded
                ) { clickedEntity ->

                    expandedCategoryId = if (expandedCategoryId == clickedEntity.categoryId) {
                        null
                    } else {
                        clickedEntity.categoryId
                    }

                    notifyDataSetChanged()
                }
            }

            is CategoryUiItem.AddButtonItem -> (holder as AddViewHolder).bind()
        }
    }


    class AddViewHolder(
        private val binding: ItemCategoryAddBinding, private val onAddClick: () -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            binding.cardAdd.setOnClickListener { onAddClick() }
        }
    }

    class CategoryViewHolder(
        private val binding: ItemCategorySampleBinding,
        private val onEditCategory: (CategoryEntity) -> Unit,
        private val onDeleteCategory: (CategoryEntity) -> Unit,
        private val onAddKeyword: (CategoryEntity) -> Unit,
        private val onDeleteKeyword: (String, CategoryEntity) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            category: CategoryEntity,
            synonyms: List<String>,
            isExpanded: Boolean,
            onToggleExpand: (CategoryEntity) -> Unit
        ) = with(binding) {

            tvCategoryName.text = category.categoryName
            val image = category.categoryImage
            val context = root.context

            when {
                image.isNullOrEmpty() -> Glide.with(context).load(R.drawable.ic_car).circleCrop()
                    .into(ivCategoryIcon)

                File(image).exists() -> Glide.with(context).load(File(image))
                    .placeholder(R.drawable.ic_car).error(R.drawable.ic_car).circleCrop()
                    .into(ivCategoryIcon)

                else -> {
                    val resId =
                        context.resources.getIdentifier(image, "drawable", context.packageName)
                    Glide.with(context).load(if (resId != 0) resId else R.drawable.ic_car)
                        .circleCrop().into(ivCategoryIcon)
                }
            }

            cardCategoryIcon.setCardBackgroundColor(getCategoryColor(category.categoryName))

            if (isExpanded) {
                layoutExpandedContent.visibility = View.VISIBLE
                chipGroupSynonyms.removeAllViews()

                if (synonyms.isEmpty()) {
                    val emptyChip = Chip(context).apply { text = "No keywords learned" }
                    chipGroupSynonyms.addView(emptyChip)
                } else {
                    synonyms.forEach { word ->
                        val chip = Chip(context).apply {
                            text = word
                            isCloseIconVisible = true
                            setOnCloseIconClickListener {
                                onDeleteKeyword(word, category)
                            }
                        }
                        chipGroupSynonyms.addView(chip)
                    }
                }

                // Setup Add Button
                btnAddKeyword.setOnClickListener {
                    onAddKeyword(category)
                }

            } else {
                layoutExpandedContent.visibility = View.GONE
            }

            root.setOnClickListener {
                TransitionManager.beginDelayedTransition(root as ViewGroup, AutoTransition())
                onToggleExpand(category)
            }

            // Long Press Options
            cardCategoryIcon.setOnLongClickListener {
                val isPredefined = image != null && !File(image).exists()

                if (isPredefined) {
                    MaterialAlertDialogBuilder(context).setTitle("System Category")
                        .setMessage("Predefined categories cannot be modified.")
                        .setPositiveButton("OK", null).show()
                } else {
                    MaterialAlertDialogBuilder(context).setTitle("Category Options")
                        .setMessage("Manage '${category.categoryName}'?")
                        .setPositiveButton("Edit") { _, _ -> onEditCategory(category) }
                        .setNegativeButton("Delete") { _, _ -> onDeleteCategory(category) }
                        .setNeutralButton("Cancel", null).show()
                }
                true
            }
        }

        private fun getCategoryColor(name: String): Int {
            return try {
                when (name) {
                    "Shopping" -> "#F06292".toColorInt()
                    "Salary" -> "#64B5F6".toColorInt()
                    "Food" -> "#81C784".toColorInt()
                    "Travel" -> "#FFD54F".toColorInt()
                    "Rent" -> "#BA68C8".toColorInt()
                    "Insurance" -> "#4DB6AC".toColorInt()
                    else -> Color.LTGRAY
                }
            } catch (e: Exception) {
                Color.LTGRAY
            }
        }
    }
}