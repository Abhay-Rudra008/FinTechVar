package com.rudra.fintechvar.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.rudra.fintechvar.R
import com.rudra.fintechvar.prefs.AppPrefs
import com.rudra.fintechvar.utils.BaseActivity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.os.Build
import android.view.animation.AnimationUtils
import com.rudra.fintechvar.databinding.ActivitySplashBinding


@SuppressLint("CustomSplashScreen")
class SplashActivity : BaseActivity() {
    private lateinit var binding: ActivitySplashBinding
    private var splashJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        startLogoAnimation()

        // Handle Navigation Logic
        handleSplashFlow()
    }

    private fun startLogoAnimation() {
        val logoAnim = AnimationUtils.loadAnimation(this, R.anim.splash_anim)
        binding.ivLogo.startAnimation(logoAnim)
    }

    private fun handleSplashFlow() {
        val biometricEnabled = AppPrefs.isBiometricEnabled(this)

        if (biometricEnabled) {
            binding.progressBar.visibility = android.view.View.GONE
            checkAndShowBiometric()
        } else {
            startSplashDelay()
        }
    }

    private fun startSplashDelay() {
        splashJob = lifecycleScope.launch {
            delay(1500L) // Wait 1.5 seconds so user can see the animation
            goToMain()
        }
    }

    private fun checkAndShowBiometric() {
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        if (biometricManager.canAuthenticate(authenticators) != BiometricManager.BIOMETRIC_SUCCESS) {
            AppPrefs.setBiometricEnabled(this, false)

            binding.progressBar.visibility = android.view.View.VISIBLE
            startSplashDelay()
            return
        }

        showBiometricPrompt(authenticators)
    }

    private fun showBiometricPrompt(authenticators: Int) {
        val biometricPrompt = BiometricPrompt(
            this, ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    goToMain()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    // If they cancel or fail, kick them out.
                    finishAffinity()
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock FinWise")
            .setSubtitle("Authenticate to access your finances")
            .setAllowedAuthenticators(authenticators)
            .setConfirmationRequired(false)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun goToMain() {
        if (isFinishing) return
        startActivity(Intent(this, MainActivity::class.java))

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                OVERRIDE_TRANSITION_OPEN,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }

        finish()
    }

    override fun onDestroy() {
        splashJob?.cancel()
        super.onDestroy()
    }
}