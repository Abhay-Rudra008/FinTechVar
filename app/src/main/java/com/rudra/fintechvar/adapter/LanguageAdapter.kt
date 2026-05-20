package com.rudra.fintechvar.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rudra.fintechvar.db.modal.LanguageItemBottom
import com.rudra.fintechvar.R

class LanguageAdapter(
    private val items: List<LanguageItemBottom>,
    private val currentLang: String,
    private val onClick: (LanguageItemBottom) -> Unit
) : RecyclerView.Adapter<LanguageAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tv: TextView = view.findViewById(R.id.tvLanguage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language_sample, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tv.text = item.name

        if (item.code == currentLang) {
            holder.tv.setCompoundDrawablesWithIntrinsicBounds(
                0, 0, R.drawable.ic_check, 0
            )
        } else {
            holder.tv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
        }

        holder.itemView.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount() = items.size
}
