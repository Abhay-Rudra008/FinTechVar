package com.rudra.fintechvar.bottomfragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rudra.fintechvar.R
import com.rudra.fintechvar.adapter.CategoryMultiSelectAdapter
import com.rudra.fintechvar.db.modal.CategoryEntity

class CategoryFilterBottomSheet(
    private val categories: List<CategoryEntity>,
    private val preSelected: Set<Int>,
    private val onApply: (Set<Int>) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(
            R.layout.fragment_category_filter_bottom_sheet,
            container,
            false
        )

        val recycler = view.findViewById<RecyclerView>(R.id.recyclerCategories)
        val btnApply = view.findViewById<Button>(R.id.btnApply)
        val btnClear = view.findViewById<Button>(R.id.btnClear)

        val adapter = CategoryMultiSelectAdapter(categories, preSelected)

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        btnApply.setOnClickListener {
            onApply(adapter.getSelectedIds())
            dismiss()
        }

        btnClear.setOnClickListener {
            adapter.clearSelection()
        }

        return view
    }
}
