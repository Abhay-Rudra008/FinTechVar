package com.rudra.fintechvar.adapter


import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.rudra.fintechvar.databinding.ItemFontSampleBinding
import com.rudra.fintechvar.db.modal.FontItem


class FontAdapter(
    private val list: List<FontItem>,
    private val onClick: (FontItem) -> Unit
) : RecyclerView.Adapter<FontAdapter.FontViewHolder>() {

    inner class FontViewHolder(
        private val binding: ItemFontSampleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FontItem) {
            val context = binding.root.context

            binding.tvFontName.text = item.name

            val resId = context.resources.getIdentifier(
                item.assetName,
                "font",
                context.packageName
            )

            if (resId != 0) {
                try {
                    val typeface = ResourcesCompat.getFont(context, resId)
                    binding.tvFontName.typeface = typeface
                } catch (e: Exception) {
                    binding.tvFontName.typeface = Typeface.DEFAULT
                }
            } else {
                binding.tvFontName.typeface = Typeface.DEFAULT
            }

            binding.root.setOnClickListener {
                onClick(item)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FontViewHolder {
        val binding = ItemFontSampleBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return FontViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FontViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount() = list.size
}