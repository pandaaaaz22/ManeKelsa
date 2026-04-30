package com.manekelsa.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.manekelsa.databinding.ActivityLoginBinding
import com.manekelsa.model.UserRole
import com.manekelsa.viewmodel.AuthViewModel

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()
    private var isLoginMode = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val pass = binding.etPassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            } else if (isLoginMode) {
                viewModel.login(email, pass)
            } else {
                val name = binding.etName.text.toString().trim()
                val role = if (binding.rbWorker.isChecked) UserRole.WORKER else UserRole.CLIENT
                if (name.isEmpty()) {
                    Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                } else {
                    viewModel.register(email, pass, name, role)
                }
            }
        }

        binding.tvToggleAction.setOnClickListener {
            isLoginMode = !isLoginMode
            updateUiMode()
        }
    }

    private fun updateUiMode() {
        if (isLoginMode) {
            binding.tvTitle.text = "Welcome Back"
            binding.btnLogin.text = "Login"
            binding.tvToggleAction.text = "New here? Create an account"
            binding.tilName.visibility = View.GONE
            binding.rgRole.visibility = View.GONE
        } else {
            binding.tvTitle.text = "Create Account"
            binding.btnLogin.text = "Sign Up"
            binding.tvToggleAction.text = "Already have an account? Login"
            binding.tilName.visibility = View.VISIBLE
            binding.rgRole.visibility = View.VISIBLE
        }
    }

    private fun observeViewModel() {
        viewModel.authState.observe(this) { state ->
            binding.progressBar.isVisible = state is AuthViewModel.AuthState.Loading
            binding.btnLogin.isEnabled = state !is AuthViewModel.AuthState.Loading

            when (state) {
                is AuthViewModel.AuthState.Authenticated -> {
                    if (state.user.role == UserRole.WORKER) {
                        // Workers go to the Registration/Profile management screen
                        startActivity(Intent(this, RegisterWorkerActivity::class.java))
                    } else {
                        // Clients go to the Worker Directory
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                    finish()
                }
                is AuthViewModel.AuthState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
                }
                else -> Unit
            }
        }
    }
}
