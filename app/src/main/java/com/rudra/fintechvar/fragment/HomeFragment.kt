package com.rudra.fintechvar.fragment

import android.Manifest
import android.annotation.SuppressLint
import  com.rudra.fintechvar.R
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import com.rudra.fintechvar.activity.NotificationActivity
import com.rudra.fintechvar.adapter.TransactionAdapter
import com.rudra.fintechvar.databinding.FragmentHomeBinding
import com.rudra.fintechvar.db.database.AppDatabase
import com.rudra.fintechvar.db.repository.CategoryRepository
import com.rudra.fintechvar.db.repository.NotificationRepository
import com.rudra.fintechvar.db.viewmodal.CategoryViewModel
import com.rudra.fintechvar.db.viewmodal.ExpenseViewModel
import com.rudra.fintechvar.db.viewmodal.NotificationViewModel
import com.rudra.fintechvar.db.viewmodalfactory.CategoryViewModelFactory
import com.rudra.fintechvar.db.viewmodalfactory.NotificationViewModelFactory
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar
import android.content.res.ColorStateList
import android.util.TypedValue
import androidx.core.content.ContextCompat
import com.permissionx.guolindev.PermissionX
import com.rudra.fintechvar.bottomfragments.AddTransactionFragment
import com.rudra.fintechvar.db.repository.ExpenseRepository
import com.rudra.fintechvar.db.utils.ExpenseStatus
import com.rudra.fintechvar.db.viewmodalfactory.ExpenseViewModelFactory
import com.rudra.fintechvar.utils.DateUtilsFunctions
import com.rudra.fintechvar.voice.CommandProcessor
import com.rudra.fintechvar.voice.VaruSpeaker
import com.rudra.fintechvar.voice.VaruVoiceEngine

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var greetingJob: Job? = null

    // ViewModels
    private lateinit var notificationViewModel: NotificationViewModel
    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var expenseViewModel: ExpenseViewModel

    // Voice Engine Components
    private lateinit var speaker: VaruSpeaker
    private lateinit var voiceEngine: VaruVoiceEngine
    private lateinit var commandProcessor: CommandProcessor

    private var adapter = TransactionAdapter()


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModels()
        setupVoiceEngine()

        setupListeners()
        setupRecyclerView()
        setupTabLayout()

        observeNotifications()
        observeCategories()
        observeData()

        loadProfile()
        startGreetingUpdates()

        // Default Filter: Today
        expenseViewModel.setDateRange(DateUtilsFunctions.getStartOfDay(), Long.MAX_VALUE)
    }


    private fun setupViewModels() {
        val db = AppDatabase.getDatabase(requireContext())

        notificationViewModel = ViewModelProvider(
            this,
            NotificationViewModelFactory(NotificationRepository(db.notificationDao()))
        )[NotificationViewModel::class.java]

        categoryViewModel = ViewModelProvider(
            this,
            CategoryViewModelFactory(CategoryRepository(db.categoryDao()))
        )[CategoryViewModel::class.java]

        val expenseRepo = ExpenseRepository(db.categoryDao(),db.transactionDao())
        val expenseFactory = ExpenseViewModelFactory(expenseRepo)
        expenseViewModel =
            ViewModelProvider(requireActivity(), expenseFactory)[ExpenseViewModel::class.java]
    }

    private fun setupVoiceEngine() {
        speaker = VaruSpeaker(requireContext())

        val db = AppDatabase.getDatabase(requireContext())
        val repository = ExpenseRepository(db.categoryDao(),db.transactionDao())

        commandProcessor = CommandProcessor(
            context = requireContext(),
            repo = repository,
            scope = viewLifecycleOwner.lifecycleScope,
            speaker = speaker
        )

        voiceEngine = VaruVoiceEngine(
            context = requireContext(),
            commandProcessor = commandProcessor,
            speaker = speaker
        )
    }

    private fun setupListeners() {
        binding.notificationIcon.setOnClickListener {
            startActivity(Intent(requireContext(), NotificationActivity::class.java))
        }

        binding.addFloatingActionButton.setOnClickListener {
            AddTransactionFragment().show(childFragmentManager, "AddTransactionFragment")
        }

        binding.micFloatingActionButton.setOnClickListener {
            startVoiceInput()
        }
    }


    private fun observeNotifications() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                notificationViewModel.notificationCount.collect { count ->
                    binding.badge.text = count.toString()
                    binding.badge.isVisible = count > 0
                }
            }
        }
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                categoryViewModel.categories.collect { categories ->
                    adapter.updateCategories(categories)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun observeData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    expenseViewModel.transactions.collectLatest { list ->
                        adapter.submitList(list)
                        binding.emptyState.isVisible = list.isEmpty()
                        binding.transRecyclerView.isVisible = list.isNotEmpty()
                    }
                }

                launch {
                    expenseViewModel.incomeExpense.collectLatest { (income, expense) ->
                        binding.layoutTotals.myIncome.text = "₹ %.2f".format(income)
                        binding.layoutTotals.myExpense.text = "₹ %.2f".format(expense)


                        expenseViewModel.expenseProgress.collectLatest { state ->
                            // Set the progress and text
                            binding.layoutTotals.progressBarIncomeAndExpense.progress =
                                state.percentage
                            binding.layoutTotals.tvProgressPercent.text = "${state.percentage}%"

                            // Determine the correct color based on status
                            val colorRes = when (state.status) {
                                ExpenseStatus.SAFE -> R.color.green
                                ExpenseStatus.WARNING -> R.color.orange
                                ExpenseStatus.DANGER -> R.color.red
                            }

                            val resolvedColor = ContextCompat.getColor(requireContext(), colorRes)

                            binding.layoutTotals.progressBarIncomeAndExpense.progressTintList =
                                ColorStateList.valueOf(resolvedColor)
                            binding.layoutTotals.tvProgressPercent.setTextColor(resolvedColor)
                        }

                    }
                }

                launch {
                    expenseViewModel.allTimeIncome.collectLatest { income ->
                        binding.allTimeIncome.text = "₹ %.2f".format(income)
                    }
                }

                launch {
                    expenseViewModel.allTimeExpense.collectLatest { expense ->
                        binding.allTimeExpense.text = "₹ %.2f".format(expense)
                    }
                }

                launch {
                    expenseViewModel.allTimeProgress.collectLatest { state ->
                        // Set the progress and text
                        binding.budgetProgress.progress = state.percentage
                        binding.tvProgressPercent.text = "${state.percentage}%"

                        val colorRes = when (state.status) {
                            ExpenseStatus.SAFE -> R.color.green
                            ExpenseStatus.WARNING -> R.color.orange
                            ExpenseStatus.DANGER -> R.color.red
                        }

                        val resolvedColor = ContextCompat.getColor(requireContext(), colorRes)

                        binding.budgetProgress.progressTintList =
                            ColorStateList.valueOf(resolvedColor)
                        binding.tvProgressPercent.setTextColor(resolvedColor)
                    }
                }
            }
        }
    }


    private fun setupRecyclerView() {
        adapter = TransactionAdapter(
            onEdit = { transaction ->
                val bundle = Bundle().apply {
                    putInt("transactionId", transaction.transactionId)
                }
                AddTransactionFragment().apply {
                    arguments = bundle
                }.show(childFragmentManager, AddTransactionFragment.TAG)
            },
            onDelete = { transaction ->
                viewLifecycleOwner.lifecycleScope.launch {
                    expenseViewModel.deleteTransaction(transaction)
                    Toast.makeText(requireContext(), "Transaction deleted", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        )

        binding.transRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@HomeFragment.adapter
            setHasFixedSize(true)
        }
    }

    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val start = when (tab.position) {
                    0 -> DateUtilsFunctions.getStartOfDay()
                    1 -> DateUtilsFunctions.getStartOfWeek()
                    2 -> DateUtilsFunctions.getStartOfMonth()
                    else -> DateUtilsFunctions.getStartOfYear()
                }
                expenseViewModel.setDateRange(start, Long.MAX_VALUE)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }


    private fun startGreetingUpdates() {
        greetingJob?.cancel()
        greetingJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                binding.tvSubHeader.text = getGreeting()
                delay(60_000)
            }
        }
    }

    private fun getGreeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> getString(R.string.good_morning)
            in 12..16 -> getString(R.string.good_afternoon)
            in 17..20 -> getString(R.string.good_evening)
            else -> getString(R.string.good_night)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun loadProfile() {
        val prefs = requireContext().getSharedPreferences("profile", Context.MODE_PRIVATE)

        val name = prefs.getString("name", "").orEmpty().trim()
        val imageUriString = prefs.getString("image", null)

        val formattedName = name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }

        binding.tvWelcomeBack.text = if (name.isNotEmpty()) {
            "Welcome back, $formattedName 👋"
        } else {
            "Welcome back 👋"
        }

        if (!imageUriString.isNullOrEmpty()) {
            binding.profileImg.setImageURI(imageUriString.toUri())
            binding.profileImg.imageTintList = null
        } else {
            binding.profileImg.setImageResource(R.drawable.ic_user)
            val typedValue = TypedValue()
            requireContext().theme.resolveAttribute(
                com.google.android.material.R.attr.colorOnSurfaceVariant,
                typedValue,
                true
            )
            binding.profileImg.imageTintList = ColorStateList.valueOf(typedValue.data)
        }
    }


    private fun startVoiceInput() {
        PermissionX.init(this)
            .permissions(Manifest.permission.RECORD_AUDIO)
            .request { allGranted, _, _ ->
                if (allGranted) {
                    startListeningUI()

                    //  Start the Voice Engine
                    voiceEngine.startManualListening(
                        onResult = {
                            stopListeningUI()
                        },
                        onError = {
                            stopListeningUI()
                            Toast.makeText(
                                requireContext(),
                                "Voice input failed",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Microphone permission required",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun startListeningUI() {
        binding.micFloatingActionButton.hide()
        binding.listeningOverlay.visibility = View.VISIBLE
        binding.lottieVoiceWave.playAnimation()
    }

    private fun stopListeningUI() {
        binding.micFloatingActionButton.show()
        binding.listeningOverlay.visibility = View.GONE
        binding.lottieVoiceWave.cancelAnimation()
    }


    override fun onDestroyView() {
        greetingJob?.cancel()

        if (::voiceEngine.isInitialized) {
            voiceEngine.destroy()
        }
        if (::speaker.isInitialized) {
            speaker.release()
        }

        _binding = null
        super.onDestroyView()
    }

}