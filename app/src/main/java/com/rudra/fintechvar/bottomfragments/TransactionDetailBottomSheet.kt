package com.rudra.fintechvar.bottomfragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.rudra.fintechvar.R
import com.rudra.fintechvar.adapter.TransactionAdapter
import com.rudra.fintechvar.db.database.AppDatabase
import com.rudra.fintechvar.db.repository.CategoryRepository
import com.rudra.fintechvar.db.viewmodal.CategoryViewModel
import com.rudra.fintechvar.db.viewmodal.ExpenseViewModel
import com.rudra.fintechvar.db.viewmodalfactory.AppViewModelFactory
import com.rudra.fintechvar.db.viewmodalfactory.CategoryViewModelFactory
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class TransactionDetailBottomSheet : BottomSheetDialogFragment() {

    private val expenseViewModel: ExpenseViewModel by activityViewModels {
        AppViewModelFactory(requireActivity().application)


    }
    private var startDate: Long = 0L
    private var endDate: Long = 0L
    private val categoryViewModel: CategoryViewModel by activityViewModels {
        CategoryViewModelFactory(
            CategoryRepository(
                AppDatabase.Companion.getDatabase(requireContext()).categoryDao()
            )
        )
    }


    private lateinit var adapter: TransactionAdapter

    private lateinit var titleText: String
    private lateinit var transactionType: String
    private var categoryIds: Set<Int>? = null

    companion object {
        fun newInstance(
            title: String,
            type: String,
            startDate: Long,
            endDate: Long,
            categoryIds: Set<Int>? = null
        ) = TransactionDetailBottomSheet().apply {
            arguments = Bundle().apply {
                putString("title", title)
                putString("type", type)
                putLong("start", startDate)
                putLong("end", endDate)
                putIntegerArrayList(
                    "categories", categoryIds?.let { ArrayList(it) })
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            titleText = it.getString("title")!!
            transactionType = it.getString("type")!!
            startDate = it.getLong("start")
            endDate = it.getLong("end")

            categoryIds = it.getIntegerArrayList("categories")?.toSet()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {

        val view = inflater.inflate(
            R.layout.fragment_transaction_detail_bottom_sheet, container, false
        )

        view.findViewById<TextView>(R.id.tvCategory).text = titleText

        adapter = TransactionAdapter(
            onDelete = { expenseViewModel.deleteTransaction(it) })

        view.findViewById<RecyclerView>(R.id.rvTransactions).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@TransactionDetailBottomSheet.adapter
        }

        observeTransactions()

        return view
    }

    private fun observeTransactions() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                combine(
                    categoryViewModel.categories, expenseViewModel.getTransactionsForBottomSheet(
                        start = startDate,
                        end = endDate,
                        type = transactionType,
                        categoryIds = categoryIds
                    )
                ) { categories, transactions ->
                    categories to transactions
                }.collect { (categories, transactions) ->

                    adapter.updateCategories(categories)

                    adapter.submitList(transactions)

                    if (transactions.isEmpty()) dismiss()
                }
            }
        }
    }


}