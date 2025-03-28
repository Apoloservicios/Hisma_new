package com.hisma.app.ui.dashboard

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hisma.app.R
import com.hisma.app.databinding.ActivityDashboardBinding
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
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar la toolbar
        setSupportActionBar(binding.toolbar)

        // Mostrar la información del usuario actual
        showCurrentUserInfo()

        // Cargar información del lubricentro
        loadLubricenterInfo()

        // Configurar listeners de los botones
        setupClickListeners()

        // Observar cambios en el ViewModel
        observeViewModel()
    }

    private fun showCurrentUserInfo() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Buscar información del usuario en Firestore
            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        val role = document.getString("role") ?: "EMPLOYEE"

                        val displayName = if (name.isNotEmpty() && lastName.isNotEmpty()) {
                            "$name $lastName"
                        } else {
                            currentUser.email ?: "Usuario"
                        }

                        binding.textUserName.text = displayName

                        // Mostrar el rol del usuario
                        when (role) {
                            "SYSTEM_ADMIN" -> binding.textUserRole.text = "Admin Sistema"
                            "LUBRICENTER_ADMIN" -> binding.textUserRole.text = "Admin"
                            else -> binding.textUserRole.text = "Empleado"
                        }
                    } else {
                        binding.textUserName.text = currentUser.email ?: "Usuario"
                        binding.textUserRole.text = "Usuario"
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("DashboardActivity", "Error al obtener datos del usuario", e)
                    binding.textUserName.text = currentUser.email ?: "Usuario"
                    binding.textUserRole.text = "Usuario"
                }
        } else {
            // El usuario no está autenticado
            navigateToLogin()
        }
    }

    private fun loadLubricenterInfo() {
        viewModel.loadLubricenterInfo()
    }

    private fun setupClickListeners() {
        // Botón FAB para agregar cambio de aceite
        binding.fabAddOilChange.setOnClickListener {
            navigateToRegisterOilChange()
        }

        // Botones principales
        binding.buttonProfile.setOnClickListener {
            navigateToProfile()
        }

        binding.buttonUsers.setOnClickListener {
            // Para implementar en el futuro
            Toast.makeText(this, "Funcionalidad en desarrollo", Toast.LENGTH_SHORT).show()
        }

        binding.buttonReports.setOnClickListener {
            // Para implementar en el futuro
            Toast.makeText(this, "Funcionalidad en desarrollo", Toast.LENGTH_SHORT).show()
        }

        binding.buttonRecords.setOnClickListener {
            navigateToRecordsList()
        }
    }

    private fun observeViewModel() {
        // Observar datos del lubricentro
        viewModel.lubricenterData.observe(this) { lubricenter ->
            if (lubricenter != null) {
                binding.textLubricenterName.text = lubricenter.fantasyName ?: "Mi Lubricentro"
            } else {
                // Si no hay datos, mostrar un mensaje genérico
                binding.textLubricenterName.text = "Mi Lubricentro"

                // También podríamos verificar si necesitamos crear un lubricentro
                // checkIfLubricenterNeeded()
            }
        }
    }

    // Método opcional para verificar si es necesario crear un lubricentro
    private fun checkIfLubricenterNeeded() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            db.collection("users")
                .document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val role = document.getString("role")
                        if (role == "LUBRICENTER_ADMIN") {
                            // Es un administrador pero no tiene lubricentro
                            Toast.makeText(this,
                                "No se encontró ningún lubricentro. Por favor cree uno desde su perfil.",
                                Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }
    }

    private fun navigateToRegisterOilChange() {
        val intent = Intent(this, RegisterOilChangeActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToRecordsList() {
        val intent = Intent(this, RecordsListActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToProfile() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToSubscriptionDetails() {
        val intent = Intent(this, SubscriptionDetailsActivity::class.java)
        startActivity(intent)
    }

    private fun navigateToLogin() {
        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun logout() {
        auth.signOut()
        navigateToLogin()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_dashboard, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_profile -> {
                navigateToProfile()
                true
            }
            R.id.action_subscription -> {
                navigateToSubscriptionDetails()
                true
            }
            R.id.action_logout -> {
                logout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}