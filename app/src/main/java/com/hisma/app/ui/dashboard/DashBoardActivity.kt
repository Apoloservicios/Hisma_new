package com.hisma.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.hisma.app.R
import com.hisma.app.databinding.ActivityDashboardBinding
import com.hisma.app.domain.model.UserRole
import com.hisma.app.ui.auth.AuthActivity
import com.hisma.app.ui.oilchange.RegisterOilChangeActivity
import com.hisma.app.ui.profile.ProfileActivity
import com.hisma.app.ui.records.RecordsListActivity
import com.hisma.app.ui.subscription.SubscriptionDetailsActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDashboardBinding
    private val viewModel: DashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        viewModel.userData.observe(this) { user ->
            if (user != null) {
                binding.textUserName.text = "${user.name} ${user.lastName}"
                binding.textUserRole.text = when (user.role) {
                    UserRole.SYSTEM_ADMIN -> "Administrador del Sistema"
                    UserRole.LUBRICENTER_ADMIN -> "Administrador"
                    UserRole.EMPLOYEE -> "Empleado"
                    else -> "Empleado"
                }

                updateUIForRole(user.role)
            }
        }

        viewModel.lubricenterData.observe(this) { lubricenter ->
            if (lubricenter != null) {
                binding.textLubricenterName.text = lubricenter.fantasyName
            }
        }

        // Cargar datos del usuario
        viewModel.loadUserData()
    }

    private fun updateUIForRole(role: UserRole) {
        if (role == UserRole.SYSTEM_ADMIN || role == UserRole.LUBRICENTER_ADMIN) {
            // Administradores pueden ver todas las opciones
            binding.buttonProfile.visibility = View.VISIBLE
            binding.buttonUsers.visibility = View.VISIBLE
            binding.buttonReports.visibility = View.VISIBLE
            binding.buttonRecords.visibility = View.VISIBLE
        } else {
            // Los empleados solo pueden ver registros y agregar cambios
            binding.buttonProfile.visibility = View.GONE
            binding.buttonUsers.visibility = View.GONE
            binding.buttonReports.visibility = View.GONE
            binding.buttonRecords.visibility = View.VISIBLE
        }
    }

    private fun setupClickListeners() {
        // Botón para ir al perfil del negocio
        binding.buttonProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        // Botón para ir a la gestión de usuarios (no implementado aún)
        binding.buttonUsers.setOnClickListener {
            // Por implementar
        }

        // Botón para ir a informes (no implementado aún)
        binding.buttonReports.setOnClickListener {
            // Por implementar
        }

        // Botón para ir a la lista de registros
        binding.buttonRecords.setOnClickListener {
            startActivity(Intent(this, RecordsListActivity::class.java))
        }

        // FAB para agregar un nuevo cambio de aceite
        binding.fabAddOilChange.setOnClickListener {
            startActivity(Intent(this, RegisterOilChangeActivity::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
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
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, AuthActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
    }
}