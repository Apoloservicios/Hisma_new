// app/src/main/java/com/hisma/app/ui/auth/AuthActivity.kt
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
        // Por ahora, muestra un mensaje indicando que esta funcionalidad está en desarrollo
        android.widget.Toast.makeText(
            this,
            "Recuperación de contraseña será implementada próximamente",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
}