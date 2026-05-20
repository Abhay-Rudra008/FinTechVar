package com.rudra.fintechvar.fragment

import android.graphics.Color
import com.rudra.fintechvar.R
import android.os.Bundle
import kotlinx.coroutines.flow.combine
import android.view.LayoutInflater
import android.view.View
import com.github.mikephil.charting.data.Entry
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.google.android.material.datepicker.MaterialDatePicker
import com.rudra.fintechvar.bottomfragments.TransactionDetailBottomSheet
import com.rudra.fintechvar.databinding.FragmentStatsBinding
import com.rudra.fintechvar.db.database.AppDatabase
import com.rudra.fintechvar.db.modal.TransactionEntity
import com.rudra.fintechvar.db.repository.CategoryRepository
import com.rudra.fintechvar.db.repository.ExpenseRepository
import com.rudra.fintechvar.db.utils.TransactionType
import com.rudra.fintechvar.db.viewmodal.CategoryViewModel
import com.rudra.fintechvar.db.viewmodal.ExpenseViewModel
import com.rudra.fintechvar.db.viewmodalfactory.CategoryViewModelFactory
import com.rudra.fintechvar.db.viewmodalfactory.ExpenseViewModelFactory
import com.rudra.fintechvar.utils.DateUtilsFunctions
import kotlinx.coroutines.launch
import java.util.Calendar
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class StatsFragment : Fragment() {

    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!

    // ViewModels
    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var expenseViewModel: ExpenseViewModel

    private var allTransactions: List<TransactionEntity> = emptyList()

    private var pieTransactions: List<TransactionEntity> = emptyList()
    private var trendTransactions: List<TransactionEntity> = emptyList()

    private var categoryIdNameMap = emptyMap<Int, String>()
    private val categoryColorCache = mutableMapOf<Int, Int>()

    private var trendKeys: List<String> = emptyList()
    private var trendBuckets: Map<String, List<TransactionEntity>> = emptyMap()

    // Independent Date States
    private var pieStartDate: Long = 0L
    private var pieEndDate: Long = 0L

    private var trendStartDate: Long = 0L
    private var trendEndDate: Long = 0L

    private var selectedBucketStart: Long = 0L
    private var selectedBucketEnd: Long = 0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViewModels()
        setupCharts()
        setupDatePicker()
        setupUI()
        applyDefaultDateRange()
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
        // Pie Type Toggle
        binding.togglePieType.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            updateCategoryPie(pieTransactions, checkedId == R.id.btnExpense)
        }

        // Trend Type Toggle
        binding.toggleTrendType.addOnButtonCheckedListener { _, _, _ ->
            binding.cardPeriodCategory.visibility = View.GONE
            updateTrendChart(trendTransactions)
        }

        // Trend Duration Chips
        binding.cgTrendDuration.setOnCheckedStateChangeListener { _, checkedIds ->
            val id = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val cal = Calendar.getInstance()
            trendEndDate = System.currentTimeMillis()
            trendStartDate = when (id) {
                R.id.chip7D -> {
                    cal.add(Calendar.DAY_OF_YEAR, -7); cal.timeInMillis
                }
                R.id.chip4W -> {
                    cal.add(Calendar.DAY_OF_YEAR, -28); cal.timeInMillis
                }
                R.id.chip12M -> {
                    cal.add(Calendar.MONTH, -12); cal.timeInMillis
                }
                R.id.chip10Y -> {
                    cal.add(Calendar.YEAR, -10); cal.timeInMillis
                }
                else -> 0L
            }
            binding.cardPeriodCategory.visibility = View.GONE
            updateTrendData()
        }

        setupCategoryClick()
        setupIncomeExpenseClick()
        setupTrendBarClick()
        setupHorizontalBarClick()
    }

    private fun setupCharts() {
        val textColor = requireContext().getColor(android.R.color.tab_indicator_text)
        val hintColor = requireContext().getColor(android.R.color.darker_gray)
        val emptyText = "No transactions for this period"

        // Set up Pies
        listOf(binding.pieChart, binding.incomePieChart).forEach { chart ->
            chart.description.isEnabled = false
            chart.setDrawEntryLabels(false)
            chart.isDrawHoleEnabled = true
            chart.setHoleColor(Color.TRANSPARENT)
            chart.setCenterTextColor(textColor)
            chart.setNoDataText(emptyText)
            chart.setNoDataTextColor(hintColor)
            chart.legend.isEnabled = chart == binding.incomePieChart
            chart.holeRadius = 55f
        }

        // Set up Bars
        listOf(binding.trendBarChart, binding.periodCategoryBarChart).forEach { chart ->
            chart.description.isEnabled = false
            chart.setNoDataText(emptyText)
            chart.setNoDataTextColor(hintColor)
            chart.xAxis.position = XAxis.XAxisPosition.BOTTOM
            chart.xAxis.setDrawGridLines(false)
            chart.xAxis.textColor = textColor
            chart.axisRight.isEnabled = false
            chart.axisLeft.textColor = textColor
            chart.axisLeft.axisMinimum = 0f
            chart.legend.textColor = textColor
        }
        binding.periodCategoryBarChart.legend.isEnabled = false
    }

    private fun setupDatePicker() {
        binding.etFromDate.setOnClickListener { showDatePicker() }
        binding.etToDate.setOnClickListener { showDatePicker() }
    }

    private fun showDatePicker() {
        val picker =
            MaterialDatePicker.Builder.dateRangePicker().setTitleText("Select Date Range").build()
        picker.addOnPositiveButtonClickListener {
            pieStartDate = it.first
            pieEndDate = endOfDay(it.second)
            binding.etFromDate.setText(DateUtilsFunctions.formatDate(pieStartDate))
            binding.etToDate.setText(DateUtilsFunctions.formatDate(pieEndDate))
            updatePieData()
        }
        picker.show(parentFragmentManager, "DATE_RANGE")
    }

    private fun applyDefaultDateRange() {
        val cal = Calendar.getInstance()
        pieEndDate = System.currentTimeMillis()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        pieStartDate = cal.timeInMillis

        binding.etFromDate.setText(DateUtilsFunctions.formatDate(pieStartDate))
        binding.etToDate.setText(DateUtilsFunctions.formatDate(pieEndDate))

        binding.cgTrendDuration.check(R.id.chip12M)

        expenseViewModel.setDateRange(0L, Long.MAX_VALUE)
    }

    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    categoryViewModel.categories,
                    expenseViewModel.transactions
                ) { cats, txs -> Pair(cats, txs) }
                    .collect { (categories, txs) ->
                        allTransactions = txs
                        categoryIdNameMap = categories.associate { it.categoryId to it.categoryName }

                        updatePieData()
                        updateTrendData()
                    }
            }
        }
    }


    private fun updatePieData() {
        pieTransactions = allTransactions.filter { it.date in pieStartDate..pieEndDate }
        updateIncomeExpensePie(pieTransactions)
        updateCategoryPie(pieTransactions, binding.togglePieType.checkedButtonId == R.id.btnExpense)
    }

    private fun updateTrendData() {
        trendTransactions = allTransactions.filter { it.date in trendStartDate..trendEndDate }
        updateTrendChart(trendTransactions)
    }


    private fun updateTrendChart(transactions: List<TransactionEntity>) {
        if (transactions.isEmpty()) {
            binding.trendBarChart.clear(); return
        }

        val mode = binding.toggleTrendType.checkedButtonId
        val chipId = binding.cgTrendDuration.checkedChipId

        trendBuckets = groupTransactionsByTime(transactions, chipId)
        trendKeys = trendBuckets.keys.sorted()

        val incomeEntries = mutableListOf<BarEntry>()
        val expenseEntries = mutableListOf<BarEntry>()

        trendKeys.forEachIndexed { i, key ->
            val periodTxs = trendBuckets[key] ?: emptyList()
            val income = periodTxs.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
            val expense = periodTxs.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }

            incomeEntries.add(BarEntry(i.toFloat(), income.toFloat()))
            expenseEntries.add(BarEntry(i.toFloat(), expense.toFloat()))
        }

        val incomeSet = BarDataSet(incomeEntries, "Income").apply { color = "#66BB6A".toColorInt() }
        val expenseSet = BarDataSet(expenseEntries, "Expense").apply { color = "#EF5350".toColorInt() }

        val barData = BarData()
        if (mode == R.id.btnTrendIncome) barData.addDataSet(incomeSet)
        if (mode == R.id.btnTrendExpense) barData.addDataSet(expenseSet)

        binding.trendBarChart.apply {
            data = barData
            xAxis.valueFormatter = IndexAxisValueFormatter(trendKeys.map { formatBucketLabel(it, chipId) })
            xAxis.setCenterAxisLabels(false)
            xAxis.axisMinimum = -0.5f
            xAxis.axisMaximum = trendKeys.size - 0.5f
            animateY(500)
            invalidate()
        }
    }

    private fun setupTrendBarClick() {
        binding.trendBarChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                if (e == null) return

                val index = e.x.toInt()

                if (index in trendKeys.indices) {
                    val key = trendKeys[index]
                    val transactionsForPeriod = trendBuckets[key] ?: emptyList()
                    val chipId = binding.cgTrendDuration.checkedChipId

                    if (transactionsForPeriod.isNotEmpty()) {
                        selectedBucketStart = transactionsForPeriod.minOf { it.date }
                        selectedBucketEnd = transactionsForPeriod.maxOf { it.date }
                    } else {
                        selectedBucketStart = trendStartDate
                        selectedBucketEnd = trendEndDate
                    }

                    binding.cardPeriodCategory.visibility = View.VISIBLE
                    binding.tvPeriodCategoryTitle.text = "Categories for ${formatBucketLabel(key, chipId)}"

                    updateHorizontalCategoryChart(transactionsForPeriod)
                }
            }

            override fun onNothingSelected() {
                binding.cardPeriodCategory.visibility = View.GONE
            }
        })
    }

    private fun updateHorizontalCategoryChart(transactions: List<TransactionEntity>) {
        val mode = binding.toggleTrendType.checkedButtonId
        val targetType = if (mode == R.id.btnTrendIncome) TransactionType.INCOME.name else TransactionType.EXPENSE.name

        val grouped = transactions.filter { it.type == targetType }
            .groupBy { it.categoryId }
            .mapValues { it.value.sumOf { tx -> tx.amount }.toFloat() }
            .toList()
            .sortedBy { it.second }

        if (grouped.isEmpty()) {
            binding.periodCategoryBarChart.clear()
            return
        }

        val entries = grouped.mapIndexed { i, p -> BarEntry(i.toFloat(), p.second, p.first) }
        val labels = grouped.map { categoryIdNameMap[it.first] ?: "Unknown" }

        val dataSet = BarDataSet(entries, "Total").apply {
            colors = grouped.map { getColorForCategory(it.first) }
        }

        binding.periodCategoryBarChart.apply {
            data = BarData(dataSet)
            xAxis.valueFormatter = IndexAxisValueFormatter(labels)
            xAxis.labelCount = labels.size
            animateX(500)
            invalidate()
        }
    }

    private fun groupTransactionsByTime(txs: List<TransactionEntity>, chipId: Int): Map<String, List<TransactionEntity>> {
        val sdf = when (chipId) {
            R.id.chip12M -> SimpleDateFormat("yyyy-MM", Locale.ENGLISH)
            R.id.chip10Y -> SimpleDateFormat("yyyy", Locale.ENGLISH)
            else -> SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        }
        return txs.groupBy {
            if (chipId == R.id.chip4W) {
                val cal = Calendar.getInstance().apply { timeInMillis = it.date }
                "${cal.get(Calendar.YEAR)}-W${cal.get(Calendar.WEEK_OF_YEAR)}"
            } else sdf.format(Date(it.date))
        }
    }

    private fun setupHorizontalBarClick() {
        binding.periodCategoryBarChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val categoryId = (e as? BarEntry)?.data as? Int ?: return
                val mode = binding.toggleTrendType.checkedButtonId
                val type = if (mode == R.id.btnTrendIncome) TransactionType.INCOME.name else TransactionType.EXPENSE.name
                val categoryName = categoryIdNameMap[categoryId] ?: "Category"

                TransactionDetailBottomSheet.newInstance(
                    title = categoryName,
                    type = type,
                    startDate = selectedBucketStart,
                    endDate = selectedBucketEnd,
                    categoryIds = setOf(categoryId)
                ).show(childFragmentManager, "CatDetailHorizontal")
            }
            override fun onNothingSelected() {}
        })
    }

    private fun formatBucketLabel(key: String, chipId: Int): String {
        return try {
            when (chipId) {
                R.id.chip12M -> SimpleDateFormat("MMM", Locale.ENGLISH).format(SimpleDateFormat("yyyy-MM", Locale.ENGLISH).parse(key)!!)
                R.id.chip4W -> "W" + key.substringAfter("-W")
                R.id.chip10Y -> key
                else -> key.substring(5)
            }
        } catch (e: Exception) {
            key
        }
    }

    private fun updateIncomeExpensePie(transactions: List<TransactionEntity>) {
        val incomeAmount = transactions.filter { it.type == TransactionType.INCOME.name }.sumOf { it.amount }
        val expenseAmount = transactions.filter { it.type == TransactionType.EXPENSE.name }.sumOf { it.amount }

        val entries = mutableListOf<PieEntry>().apply {
            if (incomeAmount > 0) add(PieEntry(incomeAmount.toFloat(), "Income"))
            if (expenseAmount > 0) add(PieEntry(expenseAmount.toFloat(), "Expense"))
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = listOf("#66BB6A".toColorInt(), "#EF5350".toColorInt())
            valueTextColor = Color.WHITE; valueTextSize = 13f; sliceSpace = 2f
        }
        binding.incomePieChart.apply {
            data = PieData(dataSet); centerText = "Net Total\n₹ ${(incomeAmount - expenseAmount).toInt()}"; invalidate()
        }
    }

    private fun updateCategoryPie(transactions: List<TransactionEntity>, isExpense: Boolean) {
        val targetType = if (isExpense) TransactionType.EXPENSE.name else TransactionType.INCOME.name
        val filtered = transactions.filter { it.type == targetType && categoryIdNameMap.containsKey(it.categoryId) }

        val grouped = filtered.groupBy { it.categoryId }
        val entries = grouped.map { (id, list) ->
            PieEntry(list.sumOf { it.amount }.toFloat(), categoryIdNameMap[id] ?: "Unknown").apply { data = id }
        }

        val dataSet = PieDataSet(entries, "").apply {
            colors = grouped.keys.map { getColorForCategory(it) }
            valueTextColor = Color.WHITE; valueTextSize = 12f; sliceSpace = 2f
        }
        binding.pieChart.apply {
            data = PieData(dataSet); centerText = "₹ ${entries.sumOf { it.value.toDouble() }.toInt()}"; invalidate()
        }
    }

    private fun getColorForCategory(categoryId: Int): Int {
        return categoryColorCache.getOrPut(categoryId) {
            val colors = listOf(
                "#EF5350", "#AB47BC", "#5C6BC0", "#29B6F6",
                "#26A69A", "#66BB6A", "#FFCA28", "#FFA726"
            ).map { it.toColorInt() }
            colors[categoryId % colors.size]
        }
    }

    private fun setupIncomeExpenseClick() {
        binding.incomePieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val type = if ((e as? PieEntry)?.label == "Income") TransactionType.INCOME.name else TransactionType.EXPENSE.name
                // Use pieStartDate and pieEndDate here!
                TransactionDetailBottomSheet.newInstance(type, type, pieStartDate, pieEndDate, null)
                    .show(childFragmentManager, "Detail")
            }
            override fun onNothingSelected() {}
        })
    }

    private fun setupCategoryClick() {
        binding.pieChart.setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
            override fun onValueSelected(e: Entry?, h: Highlight?) {
                val id = (e as? PieEntry)?.data as? Int ?: return
                val type = if (binding.togglePieType.checkedButtonId == R.id.btnExpense) TransactionType.EXPENSE.name else TransactionType.INCOME.name
                // Use pieStartDate and pieEndDate here!
                TransactionDetailBottomSheet.newInstance(
                    categoryIdNameMap[id] ?: "Category", type, pieStartDate, pieEndDate, setOf(id)
                ).show(childFragmentManager, "CatDetail")
            }
            override fun onNothingSelected() {}
        })
    }

    private fun endOfDay(time: Long): Long {
        return Calendar.getInstance().apply {
            timeInMillis = time
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}