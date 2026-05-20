package com.rudra.fintechvar.bottomfragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rudra.fintechvar.adapter.FontAdapter
import com.rudra.fintechvar.databinding.FragmentFontBottomSheetBinding
import com.rudra.fintechvar.db.modal.FontItem
import com.rudra.fintechvar.utils.VariableName

class FontBottomSheetFragment(
    private val onSelected: (FontItem) -> Unit
) : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "FontBottomSheet"
    }

    private var _binding: FragmentFontBottomSheetBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFontBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.recyclerFonts.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true) // Optimizes performance
            adapter = FontAdapter(VariableName.FONTS) { selected ->
                onSelected(selected)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks
    }
}