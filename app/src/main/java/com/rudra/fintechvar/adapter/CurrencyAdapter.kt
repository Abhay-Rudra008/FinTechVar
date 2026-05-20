package com.rudra.fintechvar.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.rudra.fintechvar.databinding.ItemCurrencySampleBinding
import com.rudra.fintechvar.db.modal.CurrencyItem
import com.rudra.fintechvar.R


class CurrencyAdapter(
    private val list: List<CurrencyItem>,
    private val onClick: (CurrencyItem) -> Unit
) : RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder>() {

    inner class CurrencyViewHolder(
        private val binding: ItemCurrencySampleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CurrencyItem) {
            val context = binding.root.context

            val resId = context.resources.getIdentifier(
                item.countryCode.lowercase(),
                "drawable",
                context.packageName
            )

            if (resId != 0) {
                binding.imgFlag.setImageResource(resId)
            } else {
                binding.imgFlag.setImageResource(R.drawable.ic_flag_placeholder)
            }

            binding.tvCurrency.text = item.symbol
            binding.tvCurrencyName.text = item.name

            binding.root.setOnClickListener {
                onClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        val binding = ItemCurrencySampleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CurrencyViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size
}