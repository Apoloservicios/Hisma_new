package com.hisma.app.ui.auth

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.hisma.app.R
import com.hisma.app.databinding.ActivityAuthBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AuthActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAuthBinding
    lateinit var navController: NavController
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar navegación con manejo de nulos
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.auth_container)
        if (navHostFragment != null && navHostFragment is NavHostFragment) {
            navController = navHostFragment.navController
            // Observar eventos de navegación
            observeNavigationEvents()
        } else {
            Log.e("AuthActivity", "NavHostFragment not found! Check your layout.")
            Toast.makeText(this, "Error al inicializar la navegación", Toast.LENGTH_LONG).show()
            finish() // Cierra la actividad en caso de error
        }
    }

    private fun observeNavigationEvents() {
        viewModel.navigationEvent.observe(this) { event ->
            when (event) {
                is AuthViewModel.NavigationEvent.NavigateToLogin -> {
                    navController.navigate(R.id.loginFragment)
                }
                is AuthViewModel.NavigationEvent.NavigateToRegisterLubricenter -> {
                    navController.navigate(R.id.registerLubricenterFragment)
                }
                is AuthViewModel.NavigationEvent.NavigateToRegisterEmployee -> {
                    navController.navigate(R.id.registerEmployeeFragment)
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}