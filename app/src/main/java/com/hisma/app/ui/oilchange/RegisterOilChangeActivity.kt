package com.hisma.app.ui.oilchange

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hisma.app.R
import com.hisma.app.databinding.ActivityRegisterOilChangeBinding
import com.hisma.app.domain.model.OilChange
import com.hisma.app.domain.model.User
import com.hisma.app.domain.model.UserRole
import com.hisma.app.domain.model.VehicleType
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class RegisterOilChangeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterOilChangeBinding
    private val viewModel: RegisterOilChangeViewModel by viewModels()

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var firestore: FirebaseFirestore

    private var currentUser: User? = null
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterOilChangeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadCurrentUser()
        setupDatePicker()
        setupVehicleTypeSpinner()
        setupDropdowns()
        setupCheckboxListeners()
        setupSaveButton()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun loadCurrentUser() {
        val userId = firebaseAuth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val name = document.getString("name") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        val email = document.getString("email") ?: ""
                        val roleStr = document.getString("role") ?: "EMPLOYEE"
                        val role = try {
                            UserRole.valueOf(roleStr)
                        } catch (e: Exception) {
                            UserRole.EMPLOYEE
                        }
                        val lubricenterId = document.getString("lubricenterId") ?: ""

                        currentUser = User(
                            id = userId,
                            name = name,
                            lastName = lastName,
                            email = email,
                            role = role,
                            lubricenterId = lubricenterId
                        )

                        // No mostramos el campo de operario, se usa automáticamente el usuario actual
                        binding.layoutOperatorName.visibility = View.GONE
                    }
                }
        }
    }

    private fun setupDatePicker() {
        // Configurar la fecha actual
        updateDateDisplay()

        // DatePicker al hacer clic en el campo de fecha
        binding.editTextServiceDate.setOnClickListener {
            showDatePicker()
        }

        // Botón de "Hoy" con ícono
        binding.buttonToday.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_calendar_today, 0, 0, 0)
        binding.buttonToday.text = ""  // Quitar texto y dejar solo el ícono
        binding.buttonToday.setOnClickListener {
            calendar.time = Date()
            updateDateDisplay()
        }
    }

    private fun setupVehicleTypeSpinner() {
        val vehicleTypes = arrayOf("Auto", "Moto", "Camión")
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, vehicleTypes)
        binding.spinnerVehicleType.adapter = adapter
    }

    private fun setupDropdowns() {
        // Configurar dropdowns de marcas de vehículos
        setupDropdown(
            binding.dropdownVehicleBrand,
            arrayOf("Ford", "Chevrolet", "Toyota", "Volkswagen", "Fiat", "Renault", "Peugeot", "Honda", "Hyundai", "Otros")
        )

        // Configurar dropdowns de aceites (reducido a mitad de ancho)
        setupDropdown(
            binding.dropdownOilBrand,
            arrayOf("Mobil", "YPF", "Shell", "Castrol", "Total", "Valvoline", "Otros")
        )

        setupDropdown(
            binding.dropdownOilType,
            arrayOf("Mineral", "Sintético", "Semisintético")
        )

        setupDropdown(
            binding.dropdownOilViscosity,
            arrayOf("5W30", "10W40", "15W40", "20W50", "5W40", "0W20")
        )

        // Configurar otros dropdowns para filtros
        setupDropdown(
            binding.dropdownOilFilterNotes,
            arrayOf("Original", "Alternativo", "Primera marca")
        )

        setupDropdown(
            binding.dropdownAirFilterNotes,
            arrayOf("Original", "Alternativo", "Limpieza")
        )

        setupDropdown(
            binding.dropdownCabinFilterNotes,
            arrayOf("Original", "Alternativo", "Limpieza")
        )

        setupDropdown(
            binding.dropdownFuelFilterNotes,
            arrayOf("Original", "Alternativo")
        )

        // Aditivos
        setupDropdown(
            binding.dropdownAdditiveType,
            arrayOf("Limpiador de inyectores", "Limpiador de válvulas", "Antifricción")
        )
    }

    private fun setupDropdown(autoCompleteTextView: AutoCompleteTextView, items: Array<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, items)
        autoCompleteTextView.setAdapter(adapter)
    }

    private fun setupCheckboxListeners() {
        // Filtro de aceite
        binding.checkboxOilFilter.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutOilFilterNotes.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Filtro de aire
        binding.checkboxAirFilter.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutAirFilterNotes.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Filtro de habitáculo
        binding.checkboxCabinFilter.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutCabinFilterNotes.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Filtro de combustible
        binding.checkboxFuelFilter.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutFuelFilterNotes.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Refrigerante
        binding.checkboxCoolant.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutCoolantNotes.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Engrase
        binding.checkboxGrease.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutGreaseNotes.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Aditivo
        binding.checkboxAdditive.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutAdditiveDetails.visibility = if (isChecked) View.VISIBLE else View.GONE
            binding.layoutAdditiveNotes.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Caja
        binding.checkboxGearbox.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutGearboxNotes.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Diferencial
        binding.checkboxDifferential.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutDifferentialNotes.visibility = if (isChecked) View.VISIBLE else View.GONE
        }
    }

    private fun setupSaveButton() {
        binding.buttonRegister.setOnClickListener {
            if (validateForm()) {
                saveOilChange()
            }
        }
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Validar campos obligatorios
        if (binding.editTextCustomerName.text.toString().trim().isEmpty()) {
            binding.editTextCustomerName.error = "Campo obligatorio"
            isValid = false
        }

        if (binding.editTextVehiclePlate.text.toString().trim().isEmpty()) {
            binding.editTextVehiclePlate.error = "Campo obligatorio"
            isValid = false
        }

        if (binding.editTextCurrentKm.text.toString().trim().isEmpty()) {
            binding.editTextCurrentKm.error = "Campo obligatorio"
            isValid = false
        }

        if (binding.editTextNextChangeKm.text.toString().trim().isEmpty()) {
            binding.editTextNextChangeKm.error = "Campo obligatorio"
            isValid = false
        }

        if (binding.dropdownOilBrand.text.toString().trim().isEmpty()) {
            binding.dropdownOilBrand.error = "Campo obligatorio"
            isValid = false
        }

        if (binding.dropdownOilType.text.toString().trim().isEmpty()) {
            binding.dropdownOilType.error = "Campo obligatorio"
            isValid = false
        }

        if (binding.dropdownOilViscosity.text.toString().trim().isEmpty()) {
            binding.dropdownOilViscosity.error = "Campo obligatorio"
            isValid = false
        }

        if (binding.editTextOilQuantity.text.toString().trim().isEmpty()) {
            binding.editTextOilQuantity.error = "Campo obligatorio"
            isValid = false
        }

        return isValid
    }

    private fun saveOilChange() {
        binding.progressBar.visibility = View.VISIBLE

        val oilChange = createOilChangeData()
        val oilChangeMap = oilChangeToMap(oilChange)

        // Guardar en Firestore
        firestore.collection("lubricenters")
            .document(currentUser?.lubricenterId ?: "")
            .collection("oilChanges")
            .add(oilChangeMap)
            .addOnSuccessListener { documentReference ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Registro guardado correctamente", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error al guardar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun oilChangeToMap(oilChange: OilChange): Map<String, Any> {
        return mapOf(
            "operatorId" to oilChange.operatorId,
            "operatorName" to oilChange.operatorName,
            "serviceDate" to oilChange.serviceDate,
            "createdAt" to oilChange.createdAt,

            "customerName" to oilChange.customerName,
            "customerPhone" to oilChange.customerPhone,
            "vehicleType" to oilChange.vehicleType,
            "vehiclePlate" to oilChange.vehiclePlate,
            "vehicleBrand" to oilChange.vehicleBrand,
            "vehicleModel" to oilChange.vehicleModel,
            "vehicleYear" to oilChange.vehicleYear,

            "currentKm" to oilChange.currentKm,
            "nextChangeKm" to oilChange.nextChangeKm,
            "periodMonths" to oilChange.periodMonths,

            "oilBrand" to oilChange.oilBrand,
            "oilType" to oilChange.oilType,
            "oilViscosity" to oilChange.oilViscosity,
            "oilQuantity" to oilChange.oilQuantity,

            "oilFilter" to oilChange.oilFilter,
            "oilFilterNotes" to oilChange.oilFilterNotes,

            "airFilter" to oilChange.airFilter,
            "airFilterNotes" to oilChange.airFilterNotes,

            "cabinFilter" to oilChange.cabinFilter,
            "cabinFilterNotes" to oilChange.cabinFilterNotes,

            "fuelFilter" to oilChange.fuelFilter,
            "fuelFilterNotes" to oilChange.fuelFilterNotes,

            "coolant" to oilChange.coolant,
            "coolantNotes" to oilChange.coolantNotes,

            "grease" to oilChange.grease,
            "greaseNotes" to oilChange.greaseNotes,

            "additive" to oilChange.additive,
            "additiveType" to oilChange.additiveType,
            "additiveNotes" to oilChange.additiveNotes,

            "gearbox" to oilChange.gearbox,
            "gearboxNotes" to oilChange.gearboxNotes,

            "differential" to oilChange.differential,
            "differentialNotes" to oilChange.differentialNotes,

            "observations" to oilChange.observations
        )
    }

    private fun createOilChangeData(): OilChange {
        val serviceDate = calendar.time

        return OilChange(
            operatorId = currentUser?.id ?: "",
            operatorName = "${currentUser?.name ?: ""} ${currentUser?.lastName ?: ""}",
            serviceDate = serviceDate,
            createdAt = Date(),

            // Cliente y vehículo
            customerName = binding.editTextCustomerName.text.toString(),
            customerPhone = binding.editTextCustomerPhone.text.toString(),
            vehicleType = binding.spinnerVehicleType.selectedItem.toString(),
            vehiclePlate = binding.editTextVehiclePlate.text.toString().toUpperCase(Locale.getDefault()),
            vehicleBrand = binding.dropdownVehicleBrand.text.toString(),
            vehicleModel = binding.editTextVehicleModel.text.toString(),
            vehicleYear = binding.editTextVehicleYear.text.toString(),

            // Kilometraje
            currentKm = binding.editTextCurrentKm.text.toString().toIntOrNull() ?: 0,
            nextChangeKm = binding.editTextNextChangeKm.text.toString().toIntOrNull() ?: 0,
            periodMonths = binding.editTextPeriodMonths.text.toString().toIntOrNull() ?: 6,

            // Aceite
            oilBrand = binding.dropdownOilBrand.text.toString(),
            oilType = binding.dropdownOilType.text.toString(),
            oilViscosity = binding.dropdownOilViscosity.text.toString(),
            oilQuantity = binding.editTextOilQuantity.text.toString().toFloatOrNull() ?: 0f,

            // Filtros
            oilFilter = binding.checkboxOilFilter.isChecked,
            oilFilterNotes = if (binding.checkboxOilFilter.isChecked) binding.dropdownOilFilterNotes.text.toString() else "",

            airFilter = binding.checkboxAirFilter.isChecked,
            airFilterNotes = if (binding.checkboxAirFilter.isChecked) binding.dropdownAirFilterNotes.text.toString() else "",

            cabinFilter = binding.checkboxCabinFilter.isChecked,
            cabinFilterNotes = if (binding.checkboxCabinFilter.isChecked) binding.dropdownCabinFilterNotes.text.toString() else "",

            fuelFilter = binding.checkboxFuelFilter.isChecked,
            fuelFilterNotes = if (binding.checkboxFuelFilter.isChecked) binding.dropdownFuelFilterNotes.text.toString() else "",

            // Extras
            coolant = binding.checkboxCoolant.isChecked,
            coolantNotes = if (binding.checkboxCoolant.isChecked) binding.dropdownCoolantNotes.text.toString() else "",

            grease = binding.checkboxGrease.isChecked,
            greaseNotes = if (binding.checkboxGrease.isChecked) binding.dropdownGreaseNotes.text.toString() else "",

            additive = binding.checkboxAdditive.isChecked,
            additiveType = if (binding.checkboxAdditive.isChecked) binding.dropdownAdditiveType.text.toString() else "",
            additiveNotes = if (binding.checkboxAdditive.isChecked) binding.dropdownAdditiveNotes.text.toString() else "",

            gearbox = binding.checkboxGearbox.isChecked,
            gearboxNotes = if (binding.checkboxGearbox.isChecked) binding.dropdownGearboxNotes.text.toString() else "",

            differential = binding.checkboxDifferential.isChecked,
            differentialNotes = if (binding.checkboxDifferential.isChecked) binding.dropdownDifferentialNotes.text.toString() else "",

            // Observaciones
            observations = binding.editTextObservations.text.toString()
        )
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateDisplay()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun updateDateDisplay() {
        binding.editTextServiceDate.setText(dateFormatter.format(calendar.time))
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

// ViewModel para gestionar los datos
@dagger.hilt.android.lifecycle.HiltViewModel
class RegisterOilChangeViewModel @Inject constructor() : androidx.lifecycle.ViewModel() {
    // Aquí se pueden agregar funciones para gestionar los datos si es necesario
}