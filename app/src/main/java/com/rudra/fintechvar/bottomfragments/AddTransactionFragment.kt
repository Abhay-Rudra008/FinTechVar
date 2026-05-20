package com.rudra.fintechvar.bottomfragments

import android.R
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.rudra.fintechvar.activity.MainActivity
import com.rudra.fintechvar.databinding.FragmentAddTransactionBinding
import com.rudra.fintechvar.db.database.AppDatabase
import com.rudra.fintechvar.db.modal.CategoryEntity
import com.rudra.fintechvar.db.modal.TransactionEntity
import com.rudra.fintechvar.db.repository.CategoryRepository
import com.rudra.fintechvar.db.viewmodal.CategoryViewModel
import com.rudra.fintechvar.db.viewmodal.ExpenseViewModel
import com.rudra.fintechvar.db.viewmodalfactory.CategoryViewModelFactory
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddTransactionFragment : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "AddTransactionFragment"
        const val ARG_TRANSACTION_ID = "transactionId"

        // Transaction Types
        private const val TYPE_INCOME = "INCOME"
        private const val TYPE_EXPENSE = "EXPENSE"
    }

    private var _binding: FragmentAddTransactionBinding? = null
    private val binding get() = _binding!!

    // State Variables
    private var selectedTimestamp: Long = System.currentTimeMillis()
    private var selectedCategoryId: Int? = null
    private var selectedType: String = TYPE_EXPENSE
    private var editingTransactionId: Int? = null

    // Lists & Adapters
    private lateinit var categoryAdapter: ArrayAdapter<String>
    private var fullCategoryList: List<CategoryEntity> = emptyList()
    private var filteredCategoryList: List<CategoryEntity> = emptyList()

    // ViewModels
    private lateinit var categoryViewModel: CategoryViewModel
    private val expenseViewModel: ExpenseViewModel
        get() = (requireActivity() as MainActivity).expenseViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddTransactionBinding.inflate(inflater, container, false)
        initViewModels()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()
        setupListeners()
        setupObservers()
        checkForEditMode()
    }

    private fun initViewModels() {
        val db = AppDatabase.Companion.getDatabase(requireContext())
        val repository = CategoryRepository(db.categoryDao())
        val factory = CategoryViewModelFactory(repository)
        categoryViewModel = ViewModelProvider(this, factory)[CategoryViewModel::class.java]
    }

    private fun setupUI() {
        // Setup Category Dropdown
        categoryAdapter = ArrayAdapter(requireContext(), R.layout.simple_dropdown_item_1line)
        binding.actCategory.setAdapter(categoryAdapter)

        // Init DateTime displays
        updateDateTimeDisplays()
    }

    private fun setupListeners() {
        // Dropdown selection
        binding.actCategory.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryId = filteredCategoryList[position].categoryId
        }

        // Income/Expense Toggle
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            selectedType = when (checkedId) {
                com.rudra.fintechvar.R.id.btnIncomeAddTrans -> TYPE_INCOME
                com.rudra.fintechvar.R.id.btnExpenseAddTrans -> TYPE_EXPENSE
                else -> TYPE_EXPENSE
            }
            filterCategoriesByType()
        }

        // Date & Time Pickers
        binding.etDate.setOnClickListener { openDatePicker() }
        binding.etTime.setOnClickListener { openTimePicker() }

        // Save Button
        binding.btnSave.setOnClickListener { saveTransaction() }
    }

    private fun setupObservers() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                categoryViewModel.categories.collect { categories ->
                    fullCategoryList = categories
                    filterCategoriesByType()
                    restoreCategorySelectionIfNeeded()
                }
            }
        }
    }

    private fun checkForEditMode() {
        editingTransactionId = arguments?.getInt(ARG_TRANSACTION_ID)
        editingTransactionId?.let { id ->
            viewLifecycleOwner.lifecycleScope.launch {
                expenseViewModel.getTransactionById(id)?.let { transaction ->
                    populateFieldsForEdit(transaction)
                }
            }
        }
    }

    private fun populateFieldsForEdit(transaction: TransactionEntity) {
        binding.etAmount.setText(transaction.amount.toString())
        binding.etTransDescription.setText(transaction.description)
        selectedTimestamp = transaction.date
        selectedCategoryId = transaction.categoryId

        updateTypeSelection(transaction.type)
        updateDateTimeDisplays()
    }

    private fun updateTypeSelection(type: String) {
        selectedType = type
        when (type) {
            TYPE_INCOME -> binding.toggleGroup.check(com.rudra.fintechvar.R.id.btnIncomeAddTrans)
            TYPE_EXPENSE -> binding.toggleGroup.check(com.rudra.fintechvar.R.id.btnExpenseAddTrans)
        }
        filterCategoriesByType()
    }

    private fun filterCategoriesByType() {
        filteredCategoryList = fullCategoryList.filter { it.categoryType == selectedType }

        categoryAdapter.clear()
        categoryAdapter.addAll(filteredCategoryList.map { it.categoryName })
        categoryAdapter.notifyDataSetChanged()

        // Reset text when switching types to avoid incorrect category state
        binding.actCategory.setText("", false)
        selectedCategoryId = null
    }

    private fun restoreCategorySelectionIfNeeded() {
        selectedCategoryId?.let { id ->
            val selectedCategory = filteredCategoryList.find { it.categoryId == id }
            selectedCategory?.let {
                binding.actCategory.setText(it.categoryName, false)
            }
        }
    }

    private fun saveTransaction() {
        if (!validateInput()) return

        val amount = binding.etAmount.text.toString().toDouble()
        val description = binding.etTransDescription.text.toString().trim()

        val transaction = TransactionEntity(
            transactionId = editingTransactionId ?: 0,
            amount = amount,
            type = selectedType,
            date = selectedTimestamp,
            description = description,
            categoryId = selectedCategoryId!!
        )

        try {
            if (editingTransactionId != null) {
                expenseViewModel.updateTransaction(transaction)
            } else {
                expenseViewModel.addTransaction(transaction)
            }

            Log.d(
                TAG,
                "Successfully saved transaction: ID=${transaction.transactionId}, Type=$selectedType"
            )

            val message =
                if (editingTransactionId != null) "Transaction updated" else "Transaction added"
            showFeedback(message)
            dismiss()

        } catch (e: Exception) {
            Log.e(TAG, "Error saving transaction", e)
            showFeedback("Failed to save transaction. Please try again.")
        }
    }

    private fun validateInput(): Boolean {
        var isValid = true
        val amountStr = binding.etAmount.text?.toString()

        if (amountStr.isNullOrBlank() || (amountStr.toDoubleOrNull() ?: 0.0) <= 0) {
            binding.tilAmount.error = "Enter a valid amount"
            isValid = false
        } else {
            binding.tilAmount.error = null
        }

        if (selectedCategoryId == null) {
            binding.tilCategory.error = "Please select a category"
            isValid = false
        } else {
            binding.tilCategory.error = null
        }

        return isValid
    }

    private fun openDatePicker() {
        val datePicker = MaterialDatePicker.Builder.datePicker().setTitleText("Select Date")
            .setSelection(selectedTimestamp).build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance()
            calendar.timeInMillis = selection

            // Preserve the existing time from selectedTimestamp
            val existingTime = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }
            calendar.set(Calendar.HOUR_OF_DAY, existingTime.get(Calendar.HOUR_OF_DAY))
            calendar.set(Calendar.MINUTE, existingTime.get(Calendar.MINUTE))

            selectedTimestamp = calendar.timeInMillis
            updateDateTimeDisplays()
        }
        datePicker.show(childFragmentManager, "DATE_PICKER")
    }

    private fun openTimePicker() {
        val calendar = Calendar.getInstance().apply { timeInMillis = selectedTimestamp }

        val timePicker = MaterialTimePicker.Builder().setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(calendar.get(Calendar.HOUR_OF_DAY)).setMinute(calendar.get(Calendar.MINUTE))
            .setTitleText("Select Time").build()

        timePicker.addOnPositiveButtonClickListener {
            calendar.set(Calendar.HOUR_OF_DAY, timePicker.hour)
            calendar.set(Calendar.MINUTE, timePicker.minute)
            selectedTimestamp = calendar.timeInMillis
            updateDateTimeDisplays()
        }
        timePicker.show(childFragmentManager, "TIME_PICKER")
    }

    private fun updateDateTimeDisplays() {
        val dateForm = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val timeForm = SimpleDateFormat("hh:mm a", Locale.getDefault())

        binding.etDate.setText(dateForm.format(Date(selectedTimestamp)))
        binding.etTime.setText(timeForm.format(Date(selectedTimestamp)))
    }

    private fun showFeedback(message: String) {
        val view = activity?.findViewById<View>(R.id.content) ?: binding.root
        Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}