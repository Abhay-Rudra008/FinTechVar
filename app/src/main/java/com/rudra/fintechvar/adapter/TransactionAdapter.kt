package com.rudra.fintechvar.adapter

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.ColorStateList
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rudra.fintechvar.databinding.ItemTransactionBinding
import com.rudra.fintechvar.db.modal.CategoryEntity
import com.rudra.fintechvar.db.modal.TransactionEntity
import com.rudra.fintechvar.R
import com.rudra.fintechvar.utils.DateUtilsFunctions


class TransactionAdapter(
    private val onEdit: (TransactionEntity) -> Unit = {},
    private val onDelete: (TransactionEntity) -> Unit = {}
) : ListAdapter<TransactionEntity, TransactionAdapter.TransactionViewHolder>(DiffCallback()) {

    private var categoryMap: Map<Int, CategoryEntity> = emptyMap()


    inner class TransactionViewHolder(
        private val binding: ItemTransactionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        @SuppressLint("SetTextI18n")
        fun bind(transaction: TransactionEntity) {
            val context = binding.root.context
            val category = categoryMap[transaction.categoryId]

            binding.transName.text = category?.categoryName ?: "Unknown"
            binding.transAmount.text = "₹${transaction.amount}"
            val isIncome = transaction.type.equals("INCOME", ignoreCase = true)
            if (isIncome) {
                binding.transType.setImageResource(R.drawable.ic_income)
                val greenColor = ContextCompat.getColor(context, R.color.green)
                binding.transType.imageTintList = ColorStateList.valueOf(greenColor)
            } else {
                binding.transType.setImageResource(R.drawable.ic_expense)
                val redColor = ContextCompat.getColor(context, R.color.red)
                binding.transType.imageTintList = ColorStateList.valueOf(redColor)
            }
            binding.dateAndTime.text = DateUtilsFunctions.formatSmartDateTime(transaction.date)


            // Image logic
            val image = category?.categoryImage
            if (!image.isNullOrEmpty()) {
                if (image.startsWith("content://") || image.startsWith("file://")) {
                    binding.transImage.setImageURI(image.toUri())
                } else {
                    val resId =
                        context.resources.getIdentifier(image, "drawable", context.packageName)
                    binding.transImage.setImageResource(if (resId != 0) resId else R.drawable.ic_car)
                }
            } else {
                binding.transImage.setImageResource(R.drawable.ic_car)
            }


            val descriptionText = transaction.description
            val hasDescription = !descriptionText.isNullOrBlank()

            binding.tvDescription.visibility = View.GONE
            binding.tvDescription.text = descriptionText

            if (hasDescription) {
                binding.root.setOnClickListener {
                    val isCurrentlyVisible = binding.tvDescription.isVisible

                    TransitionManager.beginDelayedTransition(
                        binding.root as ViewGroup,
                        AutoTransition()
                    )

                    // Toggle visibility
                    binding.tvDescription.visibility =
                        if (isCurrentlyVisible) View.GONE else View.VISIBLE
                }
            } else {
                binding.root.setOnClickListener(null)
            }

            binding.root.setOnLongClickListener {
                AlertDialog.Builder(context)
                    .setTitle("Transaction Options")
                    .setMessage("Edit or delete this transaction?")
                    .setPositiveButton("Edit") { _, _ -> onEdit(transaction) }
                    .setNegativeButton("Delete") { _, _ -> onDelete(transaction) }
                    .setNeutralButton("Cancel", null)
                    .show()
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        TransactionViewHolder(
            ItemTransactionBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun updateCategories(categories: List<CategoryEntity>) {
        categoryMap = categories.associateBy { it.categoryId }
        submitList(currentList) // force rebind via DiffUtil
    }

    class DiffCallback : DiffUtil.ItemCallback<TransactionEntity>() {
        override fun areItemsTheSame(old: TransactionEntity, new: TransactionEntity) =
            old.transactionId == new.transactionId

        override fun areContentsTheSame(old: TransactionEntity, new: TransactionEntity) =
            old == new
    }
}
