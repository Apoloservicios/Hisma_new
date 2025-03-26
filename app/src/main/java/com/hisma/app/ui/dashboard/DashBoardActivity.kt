package com.hisma.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.hisma.app.R
import com.hisma.app.databinding.ActivityDashboardBinding
import com.hisma.app.domain.model.SubscriptionStatus
import com.hisma.app.ui.auth.AuthActivity
import com.hisma.app.ui.profile.ProfileActivity
import com.hisma.app.ui.records.RecordsListActivity
import com.hisma.app.ui.subscription.SubscriptionDetailsActivity
import com.hisma.app.ui.subscription.SubscriptionExpiredActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar toolbar
        setSupportActionBar(binding.toolbar)

        // Configurar acciones de botones
        setupButtonListeners()

        // Observar datos del lubricentro
        observeLubricenterData()

        // Observar datos de suscripción
        observeSubscriptionData()

        // Observar eventos de navegación
        observeNavigationEvents()
    }

    private fun setupButtonListeners() {
        // Botón para registrar cambio de aceite
        binding.buttonRegisterOilChange.setOnClickListener {
            Toast.makeText(
                this,
                "Funcionalidad de registro de cambio de aceite será implementada próximamente",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Botón del FAB para registrar cambio rápidamente
        binding.fabAddOilChange.setOnClickListener {
            Toast.makeText(
                this,
                "Registro rápido de cambio de aceite",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Botón para ver detalles de suscripción
        binding.buttonSubscriptionDetails.setOnClickListener {
            startActivity(Intent(this, SubscriptionDetailsActivity::class.java))
        }

        // Botón para ver recorridos
        binding.buttonViewRecords.setOnClickListener {
            startActivity(Intent(this, RecordsListActivity::class.java))
        }
    }

    private fun observeLubricenterData() {
        viewModel.lubricenter.observe(this) { lubricenter ->
            binding.textLubricenterName.text = lubricenter.fantasyName
            binding.textLubricenterAddress.text = lubricenter.address
            binding.textLubricenterPhone.text = lubricenter.phone
            binding.textLubricenterEmail.text = lubricenter.email
        }
    }

    private fun observeSubscriptionData() {
        viewModel.subscription.observe(this) { subscription ->
            if (subscription != null) {
                // Actualizar estado de suscripción
                binding.textSubscriptionStatus.text = "Estado: ${subscription.status.name}"
                binding.textSubscriptionPlan.text = "Plan: ${subscription.planType.name}"

                // Calcular días restantes
                val currentTime = System.currentTimeMillis()
                val daysRemaining = TimeUnit.MILLISECONDS.toDays(subscription.endDate - currentTime)
                binding.textSubscriptionExpiry.text = "Expira en: $daysRemaining días"

                // Mostrar mensaje de alerta si quedan pocos días
                if (daysRemaining <= 3) {
                    binding.textSubscriptionExpiry.setTextColor(getColor(R.color.error_red))
                }
            } else {
                binding.textSubscriptionStatus.text = "Estado: No disponible"
                binding.textSubscriptionPlan.text = "Plan: No disponible"
                binding.textSubscriptionExpiry.text = "Expira en: No disponible"
            }
        }
    }

    private fun observeNavigationEvents() {
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
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                startActivity(Intent(this, ProfileActivity::class.java))
                true
            }
            R.id.action_subscription -> {
                startActivity(Intent(this, SubscriptionDetailsActivity::class.java))
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            android.R.id.home -> {
                onBackPressed()
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