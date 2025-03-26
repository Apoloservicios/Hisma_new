package com.hisma.app.ui.dashboard

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.hisma.app.R
import com.hisma.app.databinding.ActivityDashboardBinding
import com.hisma.app.ui.auth.AuthActivity
import com.hisma.app.ui.subscription.SubscriptionExpiredActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configuración básica
        title = "Panel de Control"

        // Observar datos del lubricentro
        viewModel.lubricenter.observe(this) { lubricenter ->
            binding.textLubricenterName.text = lubricenter.fantasyName
            binding.textLubricenterAddress.text = lubricenter.address
            binding.textLubricenterPhone.text = lubricenter.phone
            binding.textLubricenterEmail.text = lubricenter.email
        }

        // Observar eventos de navegación
        lifecycleScope.launch {
            viewModel.navigationEvent.collectLatest { event ->
                when (event) {
                    is DashboardViewModel.DashboardNavigationEvent.NavigateToSubscriptionExpired -> {
                        navigateToSubscriptionExpired(event.message)
                    }
                    is DashboardViewModel.DashboardNavigationEvent.NavigateToLogin -> {
                        navigateToLogin()
                    }
                }
            }
        }

        binding.buttonRegisterOilChange.setOnClickListener {
            Toast.makeText(
                this,
                "Funcionalidad de registro de cambio de aceite será implementada próximamente",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Agregar botón flotante para crear lubricentro
        binding.fabAddLubricenter.setOnClickListener {
            Toast.makeText(
                this,
                "La función para crear lubricentros será implementada próximamente",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        viewModel.logout()
    }

    private fun navigateToSubscriptionExpired(message: String) {
        val intent = Intent(this, SubscriptionExpiredActivity::class.java).apply {
            putExtra(SubscriptionExpiredActivity.EXTRA_MESSAGE, message)
        }
        startActivity(intent)
        finish()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}