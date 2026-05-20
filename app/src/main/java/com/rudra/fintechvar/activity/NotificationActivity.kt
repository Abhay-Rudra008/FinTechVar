package com.rudra.fintechvar.activity

import android.os.Bundle
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.rudra.fintechvar.adapter.NotificationAdapter
import com.rudra.fintechvar.databinding.ActivityNotificationBinding
import com.rudra.fintechvar.db.database.AppDatabase
import com.rudra.fintechvar.db.repository.NotificationRepository
import com.rudra.fintechvar.db.viewmodal.NotificationViewModel
import com.rudra.fintechvar.db.viewmodalfactory.NotificationViewModelFactory
import com.rudra.fintechvar.utils.BaseActivity
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModelProvider
import androidx.activity.OnBackPressedCallback


class NotificationActivity : BaseActivity() {

    private lateinit var binding: ActivityNotificationBinding
    private lateinit var viewModel: NotificationViewModel
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        val repo = NotificationRepository(database.notificationDao())
        val factory = NotificationViewModelFactory(repo)

        viewModel = ViewModelProvider(this, factory)[NotificationViewModel::class.java]

        adapter = NotificationAdapter(onMarkAsRead = { notification ->
            viewModel.markAsRead(notification)
        }, onSelectionModeChange = { isSelectionMode, selectedCount ->
            if (isSelectionMode) {
                binding.markAsRead.isVisible = false
                binding.deleteAll.isVisible = false

                binding.deleteSelected.isVisible = true
                binding.deleteSelected.text = "Delete ($selectedCount)"
            } else {
                binding.deleteSelected.isVisible = false
                binding.deleteAll.isVisible = true

                val hasUnread = viewModel.notifications.value.any { !it.isRead }
                binding.markAsRead.isVisible = hasUnread
            }
        })

        binding.notificationRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NotificationActivity)
            adapter = this@NotificationActivity.adapter
            setHasFixedSize(true)
        }

        binding.markAsRead.setOnClickListener {
            viewModel.markAllAsRead()
            Toast.makeText(this, "All notifications marked as read", Toast.LENGTH_SHORT).show()
        }

        binding.deleteAll.setOnClickListener {
            val totalNotes = viewModel.notifications.value.size
            if (totalNotes > 0) {
                Toast.makeText(this, "All notifications cleared", Toast.LENGTH_SHORT).show()
            }
        }

        binding.deleteSelected.setOnClickListener {
            val idsToDelete = adapter.selectedIds.toList()
            if (idsToDelete.isNotEmpty()) {
                viewModel.deleteSelectedNotifications(idsToDelete)
                adapter.clearSelection()
                Toast.makeText(this, "${idsToDelete.size} deleted", Toast.LENGTH_SHORT).show()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (adapter.isSelectionMode) {
                    adapter.clearSelection()
                    binding.deleteSelected.isVisible = false
                    binding.deleteAll.isVisible = true

                    val hasUnread = viewModel.notifications.value.any { !it.isRead }
                    binding.markAsRead.isVisible = hasUnread
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.notifications.collect { list ->
                    val isEmpty = list.isEmpty()

                    binding.notificationRecyclerView.isVisible = !isEmpty
                    binding.emptyNotification.isVisible = isEmpty

                    if (isEmpty) {
                        binding.topActionBar.isVisible = false
                    } else {
                        binding.topActionBar.isVisible = true
                        adapter.submitList(viewModel.mapNotifications(list))
                    }

                    if (!adapter.isSelectionMode && !isEmpty) {
                        binding.deleteAll.isVisible = true
                        val hasUnread = list.any { !it.isRead }
                        binding.markAsRead.isVisible = hasUnread
                    }
                }
            }
        }
    }
}