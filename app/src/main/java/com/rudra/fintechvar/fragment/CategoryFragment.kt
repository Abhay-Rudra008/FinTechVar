package com.rudra.fintechvar.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.rudra.fintechvar.adapter.CategoryAdapter
import com.rudra.fintechvar.adapter.CategoryUiItem
import com.rudra.fintechvar.bottomfragments.AddCategoryFragment
import com.rudra.fintechvar.databinding.FragmentCategoryBinding
import com.rudra.fintechvar.db.database.AppDatabase
import com.rudra.fintechvar.db.modal.CategoryEntity
import com.rudra.fintechvar.db.repository.CategoryRepository
import com.rudra.fintechvar.db.viewmodal.CategoryViewModel
import com.rudra.fintechvar.db.viewmodalfactory.CategoryViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.EditText
import android.widget.Toast
import com.rudra.fintechvar.db.modal.SynonymEntity


class CategoryFragment : Fragment() {

    private var _binding: FragmentCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CategoryViewModel
    private lateinit var adapter: CategoryAdapter
    private lateinit var database: AppDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        observeData()
        fetchSynonyms()
    }

    private fun setupViewModel() {
        database = AppDatabase.getDatabase(requireContext())
        val repository = CategoryRepository(database.categoryDao())
        val factory = CategoryViewModelFactory(repository)
        viewModel = ViewModelProvider(requireActivity(), factory)[CategoryViewModel::class.java]
    }

    private fun setupRecyclerView() {
        adapter = CategoryAdapter(
            onAddCategory = {
                AddCategoryFragment().show(childFragmentManager, "AddCategoryBottomSheet")
            },
            onEditCategory = { category ->
                AddCategoryFragment.newInstance(category.categoryId)
                    .show(childFragmentManager, "EditCategoryBottomSheet")
            },
            onDeleteCategory = { category ->
                showDeleteCategoryAlert(category)
            },
            onAddKeyword = { category ->
                showAddKeywordDialog(category)
            },
            onDeleteKeyword = { word, category ->
                deleteKeyword(word, category)
            }
        )

        val gridLayoutManager = GridLayoutManager(requireContext(), 3)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (adapter.getItemViewType(position)) {
                    CategoryAdapter.TYPE_ADD_BUTTON -> 1
                    else -> if (adapter.isItemExpanded(position)) 3 else 1
                }
            }
        }

        binding.catRecyclerView.apply {
            layoutManager = gridLayoutManager
            adapter = this@CategoryFragment.adapter
            setHasFixedSize(true)
        }
    }


    private fun showAddKeywordDialog(category: CategoryEntity) {
        val editText = EditText(requireContext()).apply {
            hint = "e.g., netflix, shoes, petrol"
            setPadding(48, 48, 48, 48)
        }

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Add keyword to ${category.categoryName}")
            .setView(editText)
            .setPositiveButton("Add") { _, _ ->
                val newWord = editText.text.toString().trim()
                if (newWord.isNotEmpty()) {
                    saveKeywordToDatabase(newWord, category.categoryName)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveKeywordToDatabase(word: String, categoryName: String) {
        lifecycleScope.launch(Dispatchers.IO) {
            //  Save to Room Database
            database.synonymDao().insertWord(SynonymEntity(word.lowercase(), categoryName))
            fetchSynonyms()
            withContext(Dispatchers.Main) {
                Toast.makeText(requireContext(), "'$word' added!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteKeyword(word: String, category: CategoryEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Remove Keyword")
            .setMessage("Are you sure you want the AI to forget the word '$word'?")
            .setPositiveButton("Remove") { _, _ ->
                lifecycleScope.launch(Dispatchers.IO) {
                    // Delete from Room Database
                    database.synonymDao().deleteWord(word)

                    fetchSynonyms()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun fetchSynonyms() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val synonymsFromDb = database.synonymDao().getAllSynonyms()
            val grouped = synonymsFromDb.groupBy(
                keySelector = { it.categoryName },
                valueTransform = { it.rawWord }
            )
            withContext(Dispatchers.Main) {
                adapter.updateSynonyms(grouped)
            }
        }
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.categories.collect { categories ->
                    val uiItems = categories.map { CategoryUiItem.CategoryItem(it) }.toMutableList<CategoryUiItem>()
                    uiItems.add(CategoryUiItem.AddButtonItem)
                    adapter.submitList(uiItems)
                    fetchSynonyms()
                }
            }
        }
    }

    private fun showDeleteCategoryAlert(category: CategoryEntity) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Delete Category")
            .setMessage("Remove '${category.categoryName}'? This will delete associated AI keywords.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteCategory(category)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}