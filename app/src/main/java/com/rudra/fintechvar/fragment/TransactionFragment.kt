package com.rudra.fintechvar.fragment


import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.rudra.fintechvar.adapter.TransactionAdapter
import com.rudra.fintechvar.bottomfragments.CategoryFilterBottomSheet
import com.rudra.fintechvar.databinding.FragmentTransactionBinding
import com.rudra.fintechvar.db.database.AppDatabase
import com.rudra.fintechvar.db.modal.CategoryEntity
import com.rudra.fintechvar.db.repository.CategoryRepository
import com.rudra.fintechvar.db.repository.ExpenseRepository
import com.rudra.fintechvar.db.viewmodal.CategoryViewModel
import com.rudra.fintechvar.db.viewmodal.ExpenseViewModel
import com.rudra.fintechvar.db.viewmodalfactory.CategoryViewModelFactory
import com.rudra.fintechvar.db.viewmodalfactory.ExpenseViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import com.rudra.fintechvar.bottomfragments.AddTransactionFragment
import com.rudra.fintechvar.db.modal.TransactionEntity
import com.rudra.fintechvar.utils.DateUtilsFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.OutputStreamWriter


class TransactionFragment : Fragment() {

    private var _binding: FragmentTransactionBinding? = null
    private val binding get() = _binding!!

    // ViewModels
    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var expenseViewModel: ExpenseViewModel

    private lateinit var transactionAdapter: TransactionAdapter

    // Data Caches
    private var categoryList: List<CategoryEntity> = emptyList()
    private var selectedCategoryIds: Set<Int> = emptySet()

    // 🔥 Cache the currently displayed transactions for exporting
    private var currentTransactions: List<TransactionEntity> = emptyList()

    // State
    private var startDate: Long = 0L
    private var endDate: Long = 0L

    // 🔥 1. Launcher to create the CSV file (No storage permissions needed!)
    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            exportTransactionsToCSV(uri)
        } else {
            Toast.makeText(requireContext(), "Export cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTransactionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModels()
        initializeDates()
        setupUI()
        observeData()
    }

    private fun setupViewModels() {
        val db = AppDatabase.getDatabase(requireContext())

        categoryViewModel = ViewModelProvider(
            this,
            CategoryViewModelFactory(CategoryRepository(db.categoryDao()))
        )[CategoryViewModel::class.java]

        expenseViewModel = ViewModelProvider(
            requireActivity(),
            ExpenseViewModelFactory(ExpenseRepository(db.categoryDao(), db.transactionDao()))
        )[ExpenseViewModel::class.java]
    }

    private fun setupUI() {
        setupRecyclerView()
        setupDateRangePicker()
        setupCategoryFilter()
        setupDownloadButton() // 🔥 Setup export button
    }

    // ---------- DATE LOGIC ----------

    private fun initializeDates() {
        val (defaultStart, defaultEnd) = getOneMonthBeforeToToday()
        startDate = defaultStart
        endDate = defaultEnd

        binding.etFromDate.setText(DateUtilsFunctions.formatDate(startDate))
        binding.etToDate.setText(DateUtilsFunctions.formatDate(endDate))

        expenseViewModel.setDateRange(startDate, endDate)
        expenseViewModel.setCategories(emptySet())
    }

    private fun getOneMonthBeforeToToday(): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val end = cal.timeInMillis

        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val start = cal.timeInMillis

        return start to end
    }

    private fun setupDateRangePicker() {
        binding.etFromDate.setOnClickListener { showDatePicker() }
        binding.etToDate.setOnClickListener { showDatePicker() }
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Date Range")
            .build()

        picker.addOnPositiveButtonClickListener { range ->
            startDate = range.first
            endDate = range.second

            binding.etFromDate.setText(DateUtilsFunctions.formatDate(startDate))
            binding.etToDate.setText(DateUtilsFunctions.formatDate(endDate))

            expenseViewModel.setDateRange(startDate, endDate)
        }
        picker.show(parentFragmentManager, "DATE_RANGE")
    }

    // ---------- RECYCLER, FILTERS & EXPORT ----------

    private fun setupCategoryFilter() {
        binding.categoryType.setOnClickListener {
            CategoryFilterBottomSheet(
                categories = categoryList,
                preSelected = selectedCategoryIds
            ) { selected ->
                selectedCategoryIds = selected
                expenseViewModel.setCategories(selected)
                binding.categoryType.text =
                    if (selected.isEmpty()) "All Categories" else "${selected.size} Categories selected"
            }.show(childFragmentManager, "CategoryFilter")
        }
    }

    // 🔥 2. Handle Export Button Click
    private fun setupDownloadButton() {
        binding.downloadBtn.setOnClickListener {
            if (currentTransactions.isEmpty()) {
                Toast.makeText(requireContext(), "No transactions to export", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Create a smart file name based on the dates
            val fromStr = binding.etFromDate.text.toString().replace(" ", "_")
            val toStr = binding.etToDate.text.toString().replace(" ", "_")
            val fileName = "FinWise_Export_${fromStr}_to_$toStr.csv"

            // Launch the Android file picker
            createDocumentLauncher.launch(fileName)
        }
    }

    // 🔥 3. Write Data to CSV file (Works perfectly with Excel)
    private fun exportTransactionsToCSV(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                requireContext().contentResolver.openOutputStream(uri)?.use { outputStream ->
                    val writer = BufferedWriter(OutputStreamWriter(outputStream))

                    // Write Excel Headers
                    writer.write("Date,Category,Type,Amount (₹),Description\n")

                    val categoryMap = categoryList.associateBy { it.categoryId }

                    // Write rows
                    currentTransactions.forEach { tx ->
                        val dateStr = DateUtilsFunctions.formatDateTime(tx.date)
                        val categoryName = categoryMap[tx.categoryId]?.categoryName ?: "Unknown"
                        val type = tx.type
                        val amount = tx.amount.toString()

                        // We must escape quotes and commas in descriptions so it doesn't break Excel columns
                        val desc = tx.description?.replace("\"", "\"\"") ?: ""

                        // Wrap every string in quotes to ensure commas inside dates or descriptions are safe
                        val csvRow = "\"$dateStr\",\"$categoryName\",\"$type\",\"$amount\",\"$desc\"\n"
                        writer.write(csvRow)
                    }
                    writer.flush()
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Excel file saved successfully!", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(
            onEdit = { transaction ->
                val bundle = Bundle().apply { putInt("transactionId", transaction.transactionId) }
                AddTransactionFragment().apply { arguments = bundle }
                    .show(childFragmentManager, AddTransactionFragment.TAG)
            },
            onDelete = { transaction ->
                viewLifecycleOwner.lifecycleScope.launch {
                    expenseViewModel.deleteTransaction(transaction)
                    Toast.makeText(requireContext(), "Transaction deleted", Toast.LENGTH_SHORT).show()
                }
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
            setHasFixedSize(true)
        }
    }

    // ---------- OBSERVERS ----------

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    categoryViewModel.categories,
                    expenseViewModel.transactions
                ) { categories, transactions ->
                    categories to transactions
                }.collect { (categories, transactions) ->

                    categoryList = categories
                    // 🔥 Cache for export feature
                    currentTransactions = transactions

                    transactionAdapter.updateCategories(categories)
                    transactionAdapter.submitList(transactions)

                    // Toggle Empty State UI
                    if (transactions.isEmpty()) {
                        binding.recyclerView.visibility = View.GONE
                        binding.emptyState.visibility = View.VISIBLE
                    } else {
                        binding.recyclerView.visibility = View.VISIBLE
                        binding.emptyState.visibility = View.GONE
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}