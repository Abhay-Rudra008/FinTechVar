package com.rudra.fintechvar.bottomfragments

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doOnTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rudra.fintechvar.R
import com.rudra.fintechvar.adapter.CurrencyAdapter
import com.rudra.fintechvar.databinding.FragmentAddCategoryBinding
import com.rudra.fintechvar.databinding.FragmentCurrencyBottomSheetBinding
import com.rudra.fintechvar.db.database.AppDatabase
import com.rudra.fintechvar.db.modal.CategoryEntity
import com.rudra.fintechvar.db.modal.CurrencyItem
import com.rudra.fintechvar.db.repository.CategoryRepository
import com.rudra.fintechvar.db.utils.TransactionType
import com.rudra.fintechvar.db.viewmodal.CategoryViewModel
import com.rudra.fintechvar.db.viewmodalfactory.CategoryViewModelFactory
import com.yalantis.ucrop.UCrop
import kotlinx.coroutines.launch
import java.io.File

class AddCategoryFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentAddCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CategoryViewModel

    private var selectedImageUri: Uri? = null
    private var existingImagePath: String? = null
    private var editingCategoryId: Int? = null

    companion object {
        private const val ARG_CATEGORY_ID = "category_id"

        fun newInstance(categoryId: Int): AddCategoryFragment {
            return AddCategoryFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_CATEGORY_ID, categoryId)
                }
            }
        }
    }

    // Image Picker Launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { startCrop(it) }
    }

    //  Crop Image Launcher
    private val cropImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                selectedImageUri = it
                binding.ivIcon.setImageURI(it)
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR && result.data != null) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(requireContext(), "Crop error: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        checkEditMode()
        setupListeners()
    }

    private fun setupViewModel() {
        val database = AppDatabase.Companion.getDatabase(requireContext())
        val repository = CategoryRepository(database.categoryDao())
        val factory = CategoryViewModelFactory(repository)

        // Shared ViewModel
        viewModel = ViewModelProvider(requireActivity(), factory)[CategoryViewModel::class.java]
    }

    private fun checkEditMode() {
        editingCategoryId = arguments?.getInt(ARG_CATEGORY_ID)

        editingCategoryId?.let { id ->
            binding.tvTitle.text = "Edit Category" // Update title dynamically

            lifecycleScope.launch {
                val category = viewModel.getCategoryById(id)
                category?.let {
                    binding.etCategory.setText(it.categoryName)
                    existingImagePath = it.categoryImage

                    // Set Toggle Button State
                    if (it.categoryType == TransactionType.INCOME.toString()) {
                        binding.toggleGroup.check(R.id.btnIncome)
                    } else {
                        binding.toggleGroup.check(R.id.btnExpense)
                    }

                    if (!it.categoryImage.isNullOrEmpty()) {
                        if (File(it.categoryImage).exists()) {
                            binding.ivIcon.setImageURI(Uri.fromFile(File(it.categoryImage)))
                        } else {
                            val resId = resources.getIdentifier(it.categoryImage, "drawable", requireContext().packageName)
                            if (resId != 0) binding.ivIcon.setImageResource(resId)
                        }
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        // Pick image
        binding.iconContainer.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.etCategory.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrBlank()) {
                binding.tilCategoryName.error = null
            }
        }

        // Save Category
        binding.btnSave.setOnClickListener {
            val name = binding.etCategory.text.toString().trim()
            val selectedId = binding.toggleGroup.checkedButtonId

            val type = when (selectedId) {
                R.id.btnIncome -> TransactionType.INCOME.toString()
                R.id.btnExpense -> TransactionType.EXPENSE.toString()
                else -> TransactionType.EXPENSE.toString()
            }

            if (name.isEmpty()) {
                binding.tilCategoryName.error = "Enter category name"
                binding.etCategory.requestFocus()
                return@setOnClickListener
            }

            // Save Image
            val imagePath = selectedImageUri?.let { savePickedImageToInternalStorage(it) }
                ?: existingImagePath

            // Database operation
            if (editingCategoryId != null) {
                viewModel.updateCategory(
                    CategoryEntity(
                        categoryId = editingCategoryId!!,
                        categoryName = name,
                        categoryType = type,
                        categoryImage = imagePath
                    )
                )
            } else {
                viewModel.addCategory(name, type, imagePath)
            }

            dismiss()
        }
    }

    private fun startCrop(uri: Uri) {
        val destFile = File(
            requireContext().cacheDir,
            "cropped_${System.currentTimeMillis()}.jpg"
        )

        val options = UCrop.Options().apply {
            setCircleDimmedLayer(true)
            setShowCropGrid(false)
            setHideBottomControls(false)
            setToolbarTitle("Crop Category Icon")
        }

        val uCropIntent = UCrop.of(uri, Uri.fromFile(destFile))
            .withAspectRatio(1f, 1f)
            .withOptions(options)
            .getIntent(requireContext())

        cropImageLauncher.launch(uCropIntent)
    }

    private fun savePickedImageToInternalStorage(uri: Uri): String {
        val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return ""
        val file = File(requireContext().filesDir, "category_${System.currentTimeMillis()}.jpg")

        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return file.absolutePath
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

class CurrencyBottomSheetFragment(
    private val currencyList: List<CurrencyItem>,
    private val onSelected: (CurrencyItem) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: FragmentCurrencyBottomSheetBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val TAG = "CurrencyBottomSheet"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCurrencyBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        binding.recyclerCurrency.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true) // Optimizes performance

            adapter = CurrencyAdapter(currencyList) { selected ->
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