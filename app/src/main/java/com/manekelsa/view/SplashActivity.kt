package com.manekelsa.view

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.manekelsa.R
import com.manekelsa.databinding.ActivitySplashBinding

/**
 * SplashActivity — Entry point shown for 1.5 seconds on app launch.
 *
 * Responsibilities:
 *  - Display app logo, name, and tagline
 *  - Brief delay for branding visibility
 *  - Navigate to MainActivity unconditionally
 *    (authentication is optional in this version — workers are public)
 *
 * The @SuppressLint annotation suppresses the "CustomSplashScreen" warning;
 * for Android 12+ devices the OS-level splash screen displays first,
 * then this screen appears for brand reinforcement.
 */
@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    companion object {
        private const val SPLASH_DELAY_MS = 1500L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Animate the logo in
        binding.ivLogo.alpha = 0f
        binding.ivLogo.animate().alpha(1f).setDuration(800).start()

        binding.tvAppName.alpha = 0f
        binding.tvAppName.animate().alpha(1f).setDuration(800).setStartDelay(300).start()

        binding.tvTagline.alpha = 0f
        binding.tvTagline.animate().alpha(1f).setDuration(800).setStartDelay(600).start()

        // Navigate after delay
        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish() // Remove from back stack so pressing Back from Main doesn't return here
        }, SPLASH_DELAY_MS)
    }
}
