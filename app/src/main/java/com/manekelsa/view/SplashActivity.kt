package com.manekelsa.view

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.manekelsa.R
import com.manekelsa.databinding.ActivitySplashBinding
import com.manekelsa.model.UserRole
import com.manekelsa.viewmodel.AuthViewModel

/**
 * SplashActivity — Entry point shown for branding.
 * Now checks for authentication session before navigating.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val authViewModel: AuthViewModel by viewModels()

    companion object {
        private const val SPLASH_DELAY_MS = 1500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Start animations
        startAnimations()

        // Check session after delay
        Handler(Looper.getMainLooper()).postDelayed({
            checkSessionAndNavigate()
        }, SPLASH_DELAY_MS)
    }

    private fun startAnimations() {
        binding.ivLogo.alpha = 0f
        binding.ivLogo.animate().alpha(1f).setDuration(800).start()

        binding.tvAppName.alpha = 0f
        binding.tvAppName.animate().alpha(1f).setDuration(800).setStartDelay(300).start()

        binding.tvTagline.alpha = 0f
        binding.tvTagline.animate().alpha(1f).setDuration(800).setStartDelay(600).start()
    }

    private fun checkSessionAndNavigate() {
        authViewModel.authState.observe(this) { state ->
            when (state) {
                is AuthViewModel.AuthState.Authenticated -> {
                    if (state.user.role == UserRole.WORKER) {
                        startActivity(Intent(this, RegisterWorkerActivity::class.java))
                    } else {
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                    finish()
                }
                is AuthViewModel.AuthState.Idle -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                else -> Unit
            }
        }
        authViewModel.checkUserSession()
    }
}
