package com.hisma.app.ui.oilchange

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.hisma.app.R
import com.hisma.app.databinding.ActivityRegisterOilChangeBinding
import com.hisma.app.util.AutoCompleteDataProvider
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class RegisterOilChangeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterOilChangeBinding
    private val viewModel: OilChangeViewModel by viewModels()
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterOilChangeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Configurar fecha inicial
        updateDateText()

        // Configurar escuchas para campos
        setupListeners()

        // Configurar autocompletado
        setupAutoComplete()

        // Inicializar operador con el usuario actual
        initUserData()

        // Observar resultados de guardado
        observeSaveState()
    }

    private fun setupListeners() {
        // Listener para DatePicker
        binding.editTextServiceDate.setOnClickListener {
            showDatePicker()
        }

        // Botón para fecha actual
        binding.buttonToday.setOnClickListener {
            calendar.time = Date()
            updateDateText()
        }

        // Calcular automáticamente el próximo cambio (10000 km más)
        binding.editTextCurrentKm.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                try {
                    val currentKm = binding.editTextCurrentKm.text.toString().toInt()
                    // Por defecto, próximo cambio a 10000 km
                    val nextChangeKm = currentKm + 10000
                    binding.editTextNextChangeKm.setText(nextChangeKm.toString())
                } catch (e: Exception) {
                    // No hacer nada si el valor no es un número válido
                }
            }
        }

        // Validador de patente
        binding.editTextVehiclePlate.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                val plate = s.toString()
                if (plate.isNotEmpty() && !LicensePlateValidator.isValid(plate)) {
                    binding.layoutVehiclePlate.error = "Formato inválido. Use: AB123CD, ABC123 o A123BCD"
                } else {
                    binding.layoutVehiclePlate.error = null
                    // Formatear la patente si es válida
                    if (plate.isNotEmpty() && LicensePlateValidator.isValid(plate)) {
                        val formattedPlate = LicensePlateValidator.formatPlate(plate)
                        if (formattedPlate != plate) {
                            binding.editTextVehiclePlate.setText(formattedPlate)
                            binding.editTextVehiclePlate.setSelection(formattedPlate.length)
                        }
                    }
                }
            }
        })

        // Listeners para checkboxes
        setupCheckboxListeners()

        // Botón de registro
        binding.buttonRegister.setOnClickListener {
            if (validateForm()) {
                saveOilChange()
            }
        }
    }

    private fun setupCheckboxListeners() {
        // Filtro de aceite
        binding.checkboxOilFilter.setOnCheckedChangeListener { _, isChecked ->
            binding.layoutOilFilterDetails.visibility = if (isChecked) View.VISIBLE else View.GONE
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

    private fun setupAutoComplete() {
        // Marcas de vehículos
        setupAutoCompleteDropdown(
            binding.dropdownVehicleBrand,
            AutoCompleteDataProvider.ALL_VEHICLE_BRANDS
        )

        // Marcas de aceite
        setupAutoCompleteDropdown(
            binding.dropdownOilBrand,
            AutoCompleteDataProvider.OIL_BRANDS
        )

        // Tipos de aceite
        setupAutoCompleteDropdown(
            binding.dropdownOilType,
            AutoCompleteDataProvider.OIL_TYPES
        )

        // Viscosidades
        setupAutoCompleteDropdown(
            binding.dropdownOilViscosity,
            AutoCompleteDataProvider.OIL_VISCOSITIES
        )

        // Marcas de filtros
        setupAutoCompleteDropdown(
            binding.dropdownOilFilterBrand,
            AutoCompleteDataProvider.FILTER_BRANDS
        )

        // Notas comunes para filtros y extras
        val notesAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            AutoCompleteDataProvider.COMMON_NOTES
        )

        // Aplicar a todos los campos de notas
        setupAutoCompleteDropdown(binding.dropdownOilFilterNotes, AutoCompleteDataProvider.COMMON_NOTES)
        setupAutoCompleteDropdown(binding.dropdownAirFilterNotes, AutoCompleteDataProvider.COMMON_NOTES)
        setupAutoCompleteDropdown(binding.dropdownCabinFilterNotes, AutoCompleteDataProvider.COMMON_NOTES)
        setupAutoCompleteDropdown(binding.dropdownFuelFilterNotes, AutoCompleteDataProvider.COMMON_NOTES)
        setupAutoCompleteDropdown(binding.dropdownCoolantNotes, AutoCompleteDataProvider.COMMON_NOTES)
        setupAutoCompleteDropdown(binding.dropdownGreaseNotes, AutoCompleteDataProvider.COMMON_NOTES)
        setupAutoCompleteDropdown(binding.dropdownAdditiveNotes, AutoCompleteDataProvider.COMMON_NOTES)
        setupAutoCompleteDropdown(binding.dropdownGearboxNotes, AutoCompleteDataProvider.COMMON_NOTES)
        setupAutoCompleteDropdown(binding.dropdownDifferentialNotes, AutoCompleteDataProvider.COMMON_NOTES)

        // Tipo de aditivo
        setupAutoCompleteDropdown(
            binding.dropdownAdditiveType,
            AutoCompleteDataProvider.ADDITIVE_TYPES
        )
    }

    private fun <T> setupAutoCompleteDropdown(
        autoCompleteTextView: AutoCompleteTextView,
        items: List<T>
    ) {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            items
        )
        autoCompleteTextView.setAdapter(adapter)
    }

    private fun initUserData() {
        // En una implementación real, obtener el nombre del usuario actual
        viewModel.getCurrentUserName()?.let { userName ->
            binding.editTextOperatorName.setText(userName)
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                updateDateText()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateText() {
        binding.editTextServiceDate.setText(dateFormatter.format(calendar.time))
    }

    private fun validateForm(): Boolean {
        var isValid = true

        // Validación de campos obligatorios
        isValid = validateField(binding.editTextOperatorName, binding.layoutOperatorName, "Campo obligatorio") && isValid
        isValid = validateField(binding.editTextServiceDate, binding.layoutServiceDate, "Campo obligatorio") && isValid
        isValid = validateField(binding.editTextCustomerName, binding.layoutCustomerName, "Campo obligatorio") && isValid

        // Validar patente
        val plate = binding.editTextVehiclePlate.text.toString()
        if (plate.isBlank()) {
            binding.layoutVehiclePlate.error = "Campo obligatorio"
            isValid = false
        } else if (!LicensePlateValidator.isValid(plate)) {
            binding.layoutVehiclePlate.error = "Formato inválido. Use: AB123CD, ABC123 o A123BCD"
            isValid = false
        } else {
            binding.layoutVehiclePlate.error = null
        }

        // Validar kilometraje
        isValid = validateField(binding.editTextCurrentKm, binding.layoutCurrentKm, "Campo obligatorio") && isValid
        isValid = validateField(binding.editTextNextChangeKm, binding.layoutNextChangeKm, "Campo obligatorio") && isValid

        // Validar período
        isValid = validateField(binding.editTextPeriodMonths, binding.layoutPeriodMonths, "Campo obligatorio") && isValid

        // Validar aceite
        isValid = validateField(binding.dropdownOilBrand, binding.layoutOilBrand, "Seleccione una marca") && isValid
        isValid = validateField(binding.dropdownOilType, binding.layoutOilType, "Seleccione un tipo") && isValid
        isValid = validateField(binding.dropdownOilViscosity, binding.layoutOilViscosity, "Seleccione viscosidad") && isValid
        isValid = validateField(binding.editTextOilQuantity, binding.layoutOilQuantity, "Ingrese cantidad") && isValid

        // Validar campos específicos cuando están habilitados
        if (binding.checkboxOilFilter.isChecked) {
            isValid = validateField(binding.dropdownOilFilterBrand, binding.layoutOilFilterBrand, "Seleccione marca") && isValid
        }

        // Validar aditivo si está seleccionado
        if (binding.checkboxAdditive.isChecked) {
            isValid = validateField(binding.dropdownAdditiveType, binding.layoutAdditiveType, "Seleccione tipo") && isValid
        }

        return isValid
    }

    /**
     * Método auxiliar para validar campos de texto
     */
    private fun validateField(view: View, layout: com.google.android.material.textfield.TextInputLayout, errorMsg: String): Boolean {
        val text = when (view) {
            is com.google.android.material.textfield.TextInputEditText -> view.text.toString()
            is AutoCompleteTextView -> view.text.toString()
            else -> ""
        }

        if (text.isBlank()) {
            layout.error = errorMsg
            return false
        } else {
            layout.error = null
            return true
        }
    }

    private fun saveOilChange() {
        // Mostrar barra de progreso
        binding.progressBar.visibility = View.VISIBLE

        // Recolectar datos del formulario
        val oilChange = collectFormData()

        // Guardar en el ViewModel
        viewModel.saveOilChangeRecord(oilChange)
    }

    private fun collectFormData(): OilChangeData {
        return OilChangeData(
            // Datos del servicio
            operatorName = binding.editTextOperatorName.text.toString(),
            serviceDate = calendar.timeInMillis,

            // Datos del cliente/vehículo
            customerName = binding.editTextCustomerName.text.toString(),
            customerPhone = binding.editTextCustomerPhone.text.toString(),
            vehicleBrand = binding.dropdownVehicleBrand.text.toString(),
            vehicleModel = binding.editTextVehicleModel.text.toString(),
            vehiclePlate = binding.editTextVehiclePlate.text.toString(),
            vehicleYear = binding.editTextVehicleYear.text.toString().toIntOrNull() ?: 0,

            // Kilometraje y periodicidad
            currentKm = binding.editTextCurrentKm.text.toString().toIntOrNull() ?: 0,
            nextChangeKm = binding.editTextNextChangeKm.text.toString().toIntOrNull() ?: 0,
            periodMonths = binding.editTextPeriodMonths.text.toString().toIntOrNull() ?: 6,

            // Aceite
            oilBrand = binding.dropdownOilBrand.text.toString(),
            oilType = binding.dropdownOilType.text.toString(),
            oilViscosity = binding.dropdownOilViscosity.text.toString(),
            oilQuantity = binding.editTextOilQuantity.text.toString().toFloatOrNull() ?: 0f,

            // Filtros
            oilFilterChanged = binding.checkboxOilFilter.isChecked,
            oilFilterBrand = if (binding.checkboxOilFilter.isChecked) binding.dropdownOilFilterBrand.text.toString() else "",
            oilFilterNotes = if (binding.checkboxOilFilter.isChecked) binding.dropdownOilFilterNotes.text.toString() else "N/A",

            airFilterChanged = binding.checkboxAirFilter.isChecked,
            airFilterNotes = if (binding.checkboxAirFilter.isChecked) binding.dropdownAirFilterNotes.text.toString() else "N/A",

            cabinFilterChanged = binding.checkboxCabinFilter.isChecked,
            cabinFilterNotes = if (binding.checkboxCabinFilter.isChecked) binding.dropdownCabinFilterNotes.text.toString() else "N/A",

            fuelFilterChanged = binding.checkboxFuelFilter.isChecked,
            fuelFilterNotes = if (binding.checkboxFuelFilter.isChecked) binding.dropdownFuelFilterNotes.text.toString() else "N/A",

            // Extras
            coolantAdded = binding.checkboxCoolant.isChecked,
            coolantNotes = if (binding.checkboxCoolant.isChecked) binding.dropdownCoolantNotes.text.toString() else "N/A",

            greaseAdded = binding.checkboxGrease.isChecked,
            greaseNotes = if (binding.checkboxGrease.isChecked) binding.dropdownGreaseNotes.text.toString() else "N/A",

            additiveAdded = binding.checkboxAdditive.isChecked,
            additiveType = if (binding.checkboxAdditive.isChecked) binding.dropdownAdditiveType.text.toString() else "",
            additiveNotes = if (binding.checkboxAdditive.isChecked) binding.dropdownAdditiveNotes.text.toString() else "N/A",

            gearboxChecked = binding.checkboxGearbox.isChecked,
            gearboxNotes = if (binding.checkboxGearbox.isChecked) binding.dropdownGearboxNotes.text.toString() else "N/A",

            differentialChecked = binding.checkboxDifferential.isChecked,
            differentialNotes = if (binding.checkboxDifferential.isChecked) binding.dropdownDifferentialNotes.text.toString() else "N/A",

            // Observaciones
            observations = binding.editTextObservations.text.toString()
        )
    }

    private fun observeSaveState() {
        viewModel.saveState.observe(this) { state ->
            when (state) {
                is OilChangeViewModel.SaveState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                }
                is OilChangeViewModel.SaveState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Registro guardado con éxito", Toast.LENGTH_SHORT).show()
                    // Finalizar actividad y volver a la pantalla anterior
                    setResult(RESULT_OK)
                    finish()
                }
                is OilChangeViewModel.SaveState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
                null -> {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}