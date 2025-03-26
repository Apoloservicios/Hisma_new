package com.hisma.app.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.hisma.app.databinding.ActivityAuthBinding
import com.hisma.app.ui.dashboard.DashboardActivity
import dagger.hilt.android.AndroidEntryPoint


@AndroidEntryPoint
    class AuthActivity : AppCompatActivity() {

        private lateinit var binding: ActivityAuthBinding
        private val viewModel: AuthViewModel by viewModels()

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            binding = ActivityAuthBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Configuración básica
            title = "Iniciar Sesión"

            // Observar cambios en el estado del login
            viewModel.loginState.observe(this) { state ->
                when (state) {
                    is AuthViewModel.LoginState.Loading -> {
                        // Mostrar progreso
                        binding.progressBar.visibility = View.VISIBLE
                        binding.buttonLogin.isEnabled = false
                    }
                    is AuthViewModel.LoginState.Success -> {
                        // Ocultar progreso y navegar al dashboard
                        binding.progressBar.visibility = View.GONE
                        binding.buttonLogin.isEnabled = true
                        navigateToDashboard()
                    }
                    is AuthViewModel.LoginState.Error -> {
                        // Mostrar error
                        binding.progressBar.visibility = View.GONE
                        binding.buttonLogin.isEnabled = true
                        Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }

            // Configurar botón de inicio de sesión
            binding.buttonLogin.setOnClickListener {
                val email = binding.editTextEmail.text.toString()
                val password = binding.editTextPassword.text.toString()
                viewModel.login(email, password)
            }

            // Configurar texto de registro
            binding.textRegister.setOnClickListener {
                Toast.makeText(this, "Funcionalidad de registro será implementada próximamente", Toast.LENGTH_SHORT).show()
            }
        }

        private fun navigateToDashboard() {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }
    }



