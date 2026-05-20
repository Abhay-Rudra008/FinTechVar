package com.rudra.fintechvar.adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.rudra.fintechvar.databinding.ItemDateSampleBinding
import com.rudra.fintechvar.databinding.ItemNotificationSampleBinding
import com.rudra.fintechvar.db.modal.NotificationEntity
import com.rudra.fintechvar.notification.NotificationUiItem
import java.text.SimpleDateFormat
import java.util.Date
import com.rudra.fintechvar.R
import java.util.Locale
import androidx.core.view.isVisible

class NotificationAdapter(
    private val onMarkAsRead: (NotificationEntity) -> Unit,
    private val onSelectionModeChange: (isSelectionMode: Boolean, selectedCount: Int) -> Unit
) : ListAdapter<NotificationUiItem, RecyclerView.ViewHolder>(DIFF) {

    var isSelectionMode = false
        private set
    val selectedIds = mutableSetOf<Int>()

    companion object {
        private const val TYPE_DATE = 0
        private const val TYPE_NOTIFICATION = 1

        val DIFF = object : DiffUtil.ItemCallback<NotificationUiItem>() {
            override fun areItemsTheSame(a: NotificationUiItem, b: NotificationUiItem): Boolean {
                return when {
                    a is NotificationUiItem.DateHeader && b is NotificationUiItem.DateHeader -> a.title == b.title
                    a is NotificationUiItem.NotificationItem && b is NotificationUiItem.NotificationItem -> a.notification.id == b.notification.id
                    else -> false
                }
            }

            override fun areContentsTheSame(a: NotificationUiItem, b: NotificationUiItem): Boolean {
                return a == b
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is NotificationUiItem.DateHeader -> TYPE_DATE
            is NotificationUiItem.NotificationItem -> TYPE_NOTIFICATION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_DATE -> {
                val binding = ItemDateSampleBinding.inflate(inflater, parent, false)
                DateVH(binding)
            }

            else -> {
                val binding = ItemNotificationSampleBinding.inflate(inflater, parent, false)
                NotificationVH(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is NotificationUiItem.DateHeader -> (holder as DateVH).bind(item)
            is NotificationUiItem.NotificationItem -> (holder as NotificationVH).bind(item.notification)
        }
    }

    private fun toggleSelection(id: Int, position: Int) {
        if (selectedIds.contains(id)) {
            selectedIds.remove(id)
        } else {
            selectedIds.add(id)
        }

        if (selectedIds.isEmpty()) {
            isSelectionMode = false
            onSelectionModeChange(false, 0)
            notifyItemRangeChanged(0, itemCount) // Refresh all to hide checkboxes
        } else {
            onSelectionModeChange(true, selectedIds.size)
            notifyItemChanged(position) // Refresh only the clicked row to show checkmark
        }
    }

    fun clearSelection() {
        isSelectionMode = false
        selectedIds.clear()
        onSelectionModeChange(false, 0)
        notifyItemRangeChanged(0, itemCount)
    }

    class DateVH(private val binding: ItemDateSampleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: NotificationUiItem.DateHeader) {
            binding.tvDate.text = item.title
        }
    }

    inner class NotificationVH(
        private val binding: ItemNotificationSampleBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: NotificationEntity) = with(binding) {
            notificationTitle.text = notification.title
            notificationDescription.text = notification.description


            notificationTime.text =
                SimpleDateFormat("dd MMM", Locale.ENGLISH).format(Date(notification.time))

            root.setBackgroundResource(
                if (notification.isRead) R.drawable.bg_notification_read
                else R.drawable.bg_notification_unread
            )

            checkboxSelect.isVisible = isSelectionMode
            checkboxSelect.isChecked = selectedIds.contains(notification.id)

            root.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(notification.id, bindingAdapterPosition)
                } else {
                    if (!notification.isRead) {
                        onMarkAsRead(notification)
                    }
                    AlertDialog.Builder(root.context).setTitle(notification.title)
                        .setMessage(notification.description).setPositiveButton("OK", null).show()
                }
            }

            root.setOnLongClickListener {
                if (!isSelectionMode) {
                    isSelectionMode = true
                    selectedIds.add(notification.id)
                    onSelectionModeChange(true, selectedIds.size)

                    notifyItemRangeChanged(0, itemCount)
                }
                true
            }

            checkboxSelect.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(notification.id, bindingAdapterPosition)
                }
            }
        }
    }
}