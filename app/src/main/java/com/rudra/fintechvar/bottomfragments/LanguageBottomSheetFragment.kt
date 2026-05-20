package com.rudra.fintechvar.bottomfragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rudra.fintechvar.adapter.LanguageAdapter
import com.rudra.fintechvar.databinding.FragmentLanguageBottomSheetBinding
import com.rudra.fintechvar.db.modal.LanguageItemBottom
import com.rudra.fintechvar.prefs.LanguagePref
import com.rudra.fintechvar.utils.LanguageUtil


class LanguageBottomSheetFragment(
    private val onSelected: (LanguageItemBottom) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: FragmentLanguageBottomSheetBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "LanguageBottomSheet"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLanguageBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        val currentLang = LanguagePref.getLanguage(requireContext())

        binding.rvLanguages.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true) // Optimizes performance since the list size doesn't change
            adapter = LanguageAdapter(
                items = LanguageUtil.supportedLanguages,
                currentLang = currentLang
            ) { selectedLanguage ->
                onSelected(selectedLanguage)
                dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Prevent memory leaks
    }
}