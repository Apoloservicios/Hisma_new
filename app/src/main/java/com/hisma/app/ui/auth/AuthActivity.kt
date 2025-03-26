package com.hisma.app.ui.auth

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hisma.app.databinding.ActivityAuthBinding
import com.hisma.app.ui.dashboard.DashboardActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Observar eventos de navegación
        lifecycleScope.launch {
            viewModel.navigationEvent.collectLatest { event ->
                when (event) {
                    is AuthNavigationEvent.NavigateToDashboard -> navigateToDashboard()
                    is AuthNavigationEvent.NavigateToRegisterSelection -> navigateToRegisterSelection()
                    is AuthNavigationEvent.NavigateToRegisterLubricenter -> navigateToRegisterLubricenter()
                    is AuthNavigationEvent.NavigateToRegisterEmployee -> navigateToRegisterEmployee()
                    is AuthNavigationEvent.NavigateToRegister -> navigateToRegister()
                    is AuthNavigationEvent.NavigateToLogin -> navigateToLogin()
                    is AuthNavigationEvent.NavigateToForgotPassword -> navigateToForgotPassword()
                }
            }
        }

        // Mostrar fragmento de login por defecto
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(binding.authContainer.id, LoginFragment())
                .commit()
        }
    }

    private fun navigateToDashboard() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun navigateToRegisterSelection() {
        supportFragmentManager.beginTransaction()
            .replace(binding.authContainer.id, RegisterSelectionFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToRegisterLubricenter() {
        supportFragmentManager.beginTransaction()
            .replace(binding.authContainer.id, RegisterLubricenterFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToRegisterEmployee() {
        supportFragmentManager.beginTransaction()
            .replace(binding.authContainer.id, RegisterEmployeeFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToRegister() {
        supportFragmentManager.beginTransaction()
            .replace(binding.authContainer.id, RegisterFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToLogin() {
        supportFragmentManager.popBackStack()
    }

    private fun navigateToForgotPassword() {
        // Para implementar en el futuro
        android.widget.Toast.makeText(
            this,
            "Recuperación de contraseña será implementada próximamente",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}