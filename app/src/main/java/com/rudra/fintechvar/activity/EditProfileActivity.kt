package com.rudra.fintechvar.activity

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import com.rudra.fintechvar.databinding.ActivityEditProfileBinding
import com.yalantis.ucrop.UCrop
import java.io.File
import androidx.core.net.toUri
import com.rudra.fintechvar.utils.BaseActivity
import android.content.Context
import androidx.activity.enableEdgeToEdge
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doOnTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.content.res.ColorStateList
import android.util.TypedValue
import com.rudra.fintechvar.R
import com.rudra.fintechvar.utils.Permissions


class EditProfileActivity : BaseActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private var selectedImageUri: Uri? = null

    // 1. Gallery Launcher
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { startCrop(it) }
    }

    // 2. Camera Launcher
    private val takePhotoLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && selectedImageUri != null) {
            startCrop(selectedImageUri!!)
        }
    }

    // 3. Crop Launcher
    private val cropImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val resultUri = UCrop.getOutput(result.data!!)
            resultUri?.let {
                selectedImageUri = it
                binding.profileImage.setImageURI(it)
                binding.profileImage.imageTintList = null
            }
        } else if (result.resultCode == UCrop.RESULT_ERROR && result.data != null) {
            val cropError = UCrop.getError(result.data!!)
            Toast.makeText(this, "Crop failed: ${cropError?.message}", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        setupListeners()
        loadProfile()
    }

    private fun setupEdgeToEdge() {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupListeners() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.profileImageContainer.setOnClickListener { view ->
            view.isEnabled = false
            view.postDelayed({ view.isEnabled = true }, 500)

            showImagePickerDialog()
        }

        binding.etFullName.doOnTextChanged { text, _, _, _ ->
            if (!text.isNullOrBlank()) {
                binding.tilFullName.error = null
            }
        }

        binding.saveButton.setOnClickListener { saveProfile() }
    }

    private fun showImagePickerDialog() {
        val options = if (selectedImageUri != null) {
            arrayOf("Take Photo", "Choose from Gallery", "Remove Photo")
        } else {
            arrayOf("Take Photo", "Choose from Gallery")
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("Select Profile Picture")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        Permissions.checkImagePermissions(this) {
                            openCamera()
                        }
                    }
                    1 -> {
                        Permissions.checkImagePermissions(this) {
                            openGallery()
                        }
                    }
                    2 -> removeProfileImage()
                }
            }
            .show()
    }

    private fun removeProfileImage() {
        selectedImageUri = null

        binding.profileImage.setImageResource(R.drawable.ic_user)

        val typedValue = TypedValue()
        theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
        binding.profileImage.imageTintList = ColorStateList.valueOf(typedValue.data)
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun openCamera() {
        val file = File(getExternalFilesDir(null), "temp_profile.jpg")
        selectedImageUri = FileProvider.getUriForFile(
            this,
            "${packageName}.provider",
            file
        )
        selectedImageUri?.let { takePhotoLauncher.launch(it) }
    }

    private fun startCrop(uri: Uri) {
        val destFile = File(cacheDir, "cropped_${System.currentTimeMillis()}.jpg")
        val destUri = Uri.fromFile(destFile)

        val options = UCrop.Options().apply {
            setCircleDimmedLayer(true)
            setShowCropGrid(false)
            setHideBottomControls(false)
            setToolbarTitle("Crop Profile Image")
        }

        val uCropIntent = UCrop.of(uri, destUri)
            .withAspectRatio(1f, 1f)
            .withOptions(options)
            .getIntent(this)

        cropImageLauncher.launch(uCropIntent)
    }

    private fun saveProfile() {
        val name = binding.etFullName.text.toString().trim()

        if (name.isBlank()) {
            binding.tilFullName.error = "Name cannot be empty"
            binding.etFullName.requestFocus()
            return
        }

        val prefs = getSharedPreferences("profile", Context.MODE_PRIVATE)

        prefs.edit {
            putString("name", name)

            if (selectedImageUri != null) {
                putString("image", selectedImageUri.toString())
            } else {
                remove("image")
            }
        }

        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show()
        setResult(RESULT_OK)
        finish()
    }

    private fun loadProfile() {
        val prefs = getSharedPreferences("profile", Context.MODE_PRIVATE)

        binding.etFullName.setText(prefs.getString("name", ""))

        val savedImage = prefs.getString("image", null)
        if (!savedImage.isNullOrEmpty()) {
            selectedImageUri = savedImage.toUri()
            binding.profileImage.setImageURI(selectedImageUri)
            binding.profileImage.imageTintList = null
        } else {
            val typedValue = TypedValue()
            theme.resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true)
            binding.profileImage.imageTintList = ColorStateList.valueOf(typedValue.data)
        }
    }
}