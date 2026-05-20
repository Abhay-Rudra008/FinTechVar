package com.rudra.fintechvar.activity

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.rudra.fintechvar.bottomfragments.LanguageBottomSheetFragment
import com.rudra.fintechvar.bottomfragments.CurrencyBottomSheetFragment
import com.rudra.fintechvar.databinding.ActivitySettingBinding
import com.rudra.fintechvar.db.modal.CurrencyItem
import com.rudra.fintechvar.db.modal.LanguageItemBottom
import com.rudra.fintechvar.bottomfragments.FontBottomSheetFragment
import com.rudra.fintechvar.prefs.CurrencyPref
import com.rudra.fintechvar.prefs.DarkModePref
import com.rudra.fintechvar.prefs.FontPref
import com.rudra.fintechvar.utils.BaseActivity
import com.rudra.fintechvar.utils.VariableName.PREDEFINED_CURRENCY
import com.rudra.fintechvar.prefs.AppPrefs
import com.rudra.fintechvar.prefs.LanguagePref
import java.util.Locale


class SettingActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(FontPref.getFontTheme(this))
        super.onCreate(savedInstanceState)

        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupEdgeToEdge()
        initUI()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        updateCurrentLanguage()
    }


    private fun setupEdgeToEdge() {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initUI() {
        //  Load Currency
        loadSavedCurrency()

        //  Load Language
        updateCurrentLanguage()

        //  Load Font Name
        binding.tvSelectedFont.text = FontPref.getFontName(this)

        //  Load Dark Mode State
        binding.switchDarkMode.isChecked = DarkModePref.isDarkMode(this)

        //  Load Biometric State
        binding.switchBiometric.setOnCheckedChangeListener(null)
        binding.switchBiometric.isChecked = AppPrefs.isBiometricEnabled(this)
    }

    private fun setupListeners() {

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            DarkModePref.setDarkMode(this, isChecked)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Language Selection
        binding.layoutLang.setOnClickListener {
            LanguageBottomSheetFragment { language ->
                applyLanguage(language)
            }.show(supportFragmentManager, LanguageBottomSheetFragment.TAG)
        }


        // Currency Selection
        binding.layoutCurrency.setOnClickListener {
            CurrencyBottomSheetFragment(PREDEFINED_CURRENCY) { selected ->
                updateCurrencyUI(selected)
                CurrencyPref.saveCurrency(this, selected)
            }.show(supportFragmentManager, "CurrencySheet")
        }

        // Font Selection
        binding.layoutFont.setOnClickListener {
            FontBottomSheetFragment { selected ->
                FontPref.saveFont(this@SettingActivity, selected.themeRes, selected.name)
                restartApp(this@SettingActivity)
            }.show(supportFragmentManager, FontBottomSheetFragment.TAG)
        }

        //  Biometric Toggle
        binding.switchBiometric.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showBiometricSetup()
            } else {
                AppPrefs.setBiometricEnabled(this, false)
                Toast.makeText(this, "Biometrics Disabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Privacy Policy
        binding.privacyLayout.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, "https://yourapp.com/privacy-policy".toUri())
            startActivity(intent)
        }
    }


    private fun loadSavedCurrency() {
        val saved = CurrencyPref.getCurrency(this)
        updateCurrencyUI(saved)
    }

    private fun updateCurrencyUI(currency: CurrencyItem) {
        binding.tvSelectedCurrency.text = currency.symbol
        val resId = resources.getIdentifier(
            currency.countryCode.lowercase(),
            "drawable",
            packageName
        )
        if (resId != 0) {
            binding.imgCurrencyFlag.setImageResource(resId)
        }
    }

    private fun updateCurrentLanguage() {
        val code = LanguagePref.getLanguage(this)
        val locale = Locale(code)
        binding.showLang.text =
            locale.getDisplayLanguage(locale).replaceFirstChar { it.uppercase() }
    }

    private fun applyLanguage(language: LanguageItemBottom) {
        LanguagePref.setLanguage(this, language.code)
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun restartApp(context: Context) {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        val componentName = intent?.component
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        context.startActivity(mainIntent)
        Runtime.getRuntime().exit(0)
    }


    private fun showBiometricSetup() {
        val biometricManager = BiometricManager.from(this)

        if (biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            ) != BiometricManager.BIOMETRIC_SUCCESS
        ) {
            Toast.makeText(this, "Biometrics not available on this device", Toast.LENGTH_SHORT)
                .show()
            binding.switchBiometric.isChecked = false
            return
        }

        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    AppPrefs.setBiometricEnabled(this@SettingActivity, true)
                    Toast.makeText(this@SettingActivity, "Biometrics Enabled", Toast.LENGTH_SHORT)
                        .show()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    binding.switchBiometric.isChecked = false
                    Toast.makeText(
                        this@SettingActivity,
                        "Authentication failed",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Enable Biometrics")
            .setSubtitle("Confirm your identity to enable security")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(promptInfo)
    }
}