package com.hisma.app.ui.dashboard

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.hisma.app.R
import com.hisma.app.databinding.ActivityDashboardBinding
import com.hisma.app.ui.auth.AuthActivity

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
                binding.textLubricenterName.text = lubricenter.name
                binding.textLubricenterAddress.text = lubricenter.address
                binding.textLubricenterPhone.text = lubricenter.phone
                binding.textLubricenterEmail.text = lubricenter.email
            }

            // Configurar botón para registrar cambio de aceite
            binding.buttonRegisterOilChange.setOnClickListener {
                // Para implementación futura
                android.widget.Toast.makeText(
                    this,
                    "Funcionalidad de registro de cambio de aceite será implementada próximamente",
                    android.widget.Toast.LENGTH_SHORT
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
            // En una implementación real, aquí cerrarías la sesión de Firebase
            val intent = Intent(this, AuthActivity::class.java)
            startActivity(intent)
            finish()
        }
    }



