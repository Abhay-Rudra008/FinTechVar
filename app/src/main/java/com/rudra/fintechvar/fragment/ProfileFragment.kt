package com.rudra.fintechvar.fragment

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rudra.fintechvar.R
import com.rudra.fintechvar.activity.EditProfileActivity
import com.rudra.fintechvar.activity.SettingActivity
import com.rudra.fintechvar.databinding.FragmentProfileBinding
import com.rudra.fintechvar.db.database.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.widget.FrameLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.res.ColorStateList
import android.util.TypedValue
import androidx.core.content.edit


class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!


    private val editProfileLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                loadProfile()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadProfile()
        setupListeners()
    }


    private fun setupListeners() {
        // Edit Profile
        binding.layoutEditProfile.setOnClickListener {
            editProfileLauncher.launch(
                Intent(requireContext(), EditProfileActivity::class.java)
            )
        }

        // Security Info
        binding.layoutSecurity.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Your Data is Secure")
                .setMessage(
                    "We take your privacy seriously.\n\n" +
                            "• Your data is stored securely\n" +
                            "• No information is shared without your permission\n" +
                            "• Strong security measures are in place"
                )
                .setPositiveButton("Got it", null)
                .show()
        }

        // Settings
        binding.layoutSetting.setOnClickListener {
            startActivity(Intent(requireContext(), SettingActivity::class.java))
        }

        // Help Options
        binding.layoutHelp.setOnClickListener {
            val options = arrayOf("📞 Contact Support", "📧 Report an Issue")

            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Help & Support")
                .setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            val intent = Intent(Intent.ACTION_DIAL, "tel:+919876543210".toUri())
                            startActivity(intent)
                        }
                        1 -> {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:support@fintechvaru.com".toUri()
                                putExtra(Intent.EXTRA_SUBJECT, "Issue Report")
                                putExtra(Intent.EXTRA_TEXT, "Please describe your issue below:\n\n")
                            }
                            startActivity(intent)
                        }
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Delete Data
        binding.layoutDelete.setOnClickListener {
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        val context = requireContext()

        val input = EditText(context).apply {
            hint = "Type DELETE to confirm"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        }
        val container = FrameLayout(context).apply {
            val params = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(64, 24, 64, 0)
            }
            input.layoutParams = params
            addView(input)
        }

        val dialog = MaterialAlertDialogBuilder(context)
            .setTitle("Delete All Data")
            .setMessage(
                "⚠️ This action will permanently delete all your data.\n\n" +
                        "To confirm, type DELETE below."
            )
            .setView(container)
            .setPositiveButton("Delete", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.setOnShowListener {
            val deleteBtn = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE)
            deleteBtn.isEnabled = false

            input.addTextChangedListener {
                deleteBtn.isEnabled = it.toString().trim() == "DELETE"
            }

            deleteBtn.setOnClickListener {
                dialog.dismiss()
                deleteAllDataAndRestart()
            }
        }

        dialog.show()
    }

    private fun deleteAllDataAndRestart() {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            AppDatabase.getDatabase(requireContext()).close()
            requireContext().deleteDatabase("expense_db")

            requireContext()
                .getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                .edit {
                    clear()
                }

            withContext(Dispatchers.Main) {
                restartApp()
            }
        }
    }

    private fun restartApp() {
        val context = requireContext()
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)

        intent?.let {
            it.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(it)
            requireActivity().finishAffinity()
            Runtime.getRuntime().exit(0)
        }
    }

    private fun loadProfile() {
        val prefs = requireContext().getSharedPreferences("profile", Context.MODE_PRIVATE)

        binding.etName.text = prefs.getString("name", "FinTechVar")

        val imageUriString = prefs.getString("image", null)
        if (!imageUriString.isNullOrEmpty()) {
            binding.profileImage.setImageURI(imageUriString.toUri())
            binding.profileImage.imageTintList = null
        } else {
            binding.profileImage.setImageResource(R.drawable.ic_user)
            val typedValue = TypedValue()
            requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
            binding.profileImage.imageTintList = ColorStateList.valueOf(typedValue.data)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}