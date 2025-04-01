package com.hisma.app.ui.oilchange

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.hisma.app.databinding.ActivityRegisterOilChangeBinding
import com.hisma.app.domain.model.OilChangeRecord
import com.hisma.app.util.AutoCompleteDataProvider
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.*

private const val TAG = "EditOilChangeActivity"

@AndroidEntryPoint
class EditOilChangeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterOilChangeBinding
    private val viewModel: OilChangeViewModel by viewModels()
    private val calendar = Calendar.getInstance()
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // ID del registro a editar
    private var recordId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterOilChangeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar Toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Editar Cambio de Aceite"

        // Obtener ID del registro a editar
        recordId = intent.getStringExtra(EXTRA_RECORD_ID) ?: ""
        Log.d(TAG, "Editando registro con ID: $recordId")

        if (recordId.isEmpty()) {
            Toast.makeText(this, "Error: ID de registro no proporcionado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Configurar fecha inicial
        updateDateText()

        // Configurar básico
        setupBasicListeners()

        // Configurar autocompletado
        setupAutoComplete()

        // Cargar datos del registro
        loadRecordData()

        // Observar resultados de guardado
        observeSaveState()
    }

    private fun loadRecordData() {
        Log.d(TAG, "Cargando datos del registro: $recordId")
        viewModel.getOilChangeRecord(recordId)
    }

    private fun setupBasicListeners() {
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
                    // Si el campo del próximo cambio está vacío, sugerir 10000 km más
                    if (binding.editTextNextChangeKm.text.toString().isEmpty()) {
                        val nextChangeKm = currentKm + 10000
                        binding.editTextNextChangeKm.setText(nextChangeKm.toString())
                    }
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
        binding.buttonRegister.text = "Guardar Cambios"
        binding.buttonRegister.setOnClickListener {
            if (validateForm()) {
                updateOilChange()
            }
        }
    }

    private fun setupCheckboxListeners() {
        // Filtro de aceite
        binding.checkboxOilFilter.setOnCheckedChangeListener { _, isChecked ->
            // No intentamos acceder a elementos que podrían no existir
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

        // Configuración de otros checkboxes...
        // (similar a RegisterOilChangeActivity)

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

    private fun setupAutoComplete() {
        // Marcas de vehículos
        val vehicleBrandAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            AutoCompleteDataProvider.ALL_VEHICLE_BRANDS
        )
        binding.dropdownVehicleBrand.setAdapter(vehicleBrandAdapter)

        // Otros adapters (similar a RegisterOilChangeActivity)
        // Marcas de aceite
        val oilBrandAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            AutoCompleteDataProvider.OIL_BRANDS
        )
        binding.dropdownOilBrand.setAdapter(oilBrandAdapter)

        // Tipos de aceite
        val oilTypeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            AutoCompleteDataProvider.OIL_TYPES
        )
        binding.dropdownOilType.setAdapter(oilTypeAdapter)

        // Viscosidades
        val viscosityAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            AutoCompleteDataProvider.OIL_VISCOSITIES
        )
        binding.dropdownOilViscosity.setAdapter(viscosityAdapter)
    }

    private fun fillFormWithRecordData(record: OilChangeRecord) {
        Log.d(TAG, "Rellenando formulario con datos del registro: ${record.id}")

        // Datos del operario y fecha
        binding.editTextOperatorName.setText(record.createdBy)
        calendar.timeInMillis = record.createdAt
        updateDateText()

        // Datos del cliente
        binding.editTextCustomerName.setText(record.customerName)
        binding.editTextCustomerPhone.setText(record.customerPhone)

        // Datos del vehículo
        binding.dropdownVehicleBrand.setText(record.vehicleBrand)
        binding.editTextVehicleModel.setText(record.vehicleModel)
        binding.editTextVehiclePlate.setText(record.vehiclePlate)
        binding.editTextVehicleYear.setText(record.vehicleYear.toString())

        // Kilometraje
        binding.editTextCurrentKm.setText(record.kilometrage.toString())
        binding.editTextNextChangeKm.setText(record.nextChangeKm.toString())

        // Aceite
        // Parsear el tipo de aceite para obtener la viscosidad y el tipo
        val oilTypeParts = record.oilType.split(" ")
        if (oilTypeParts.isNotEmpty()) {
            binding.dropdownOilViscosity.setText(oilTypeParts[0])

            if (oilTypeParts.size > 1) {
                val remainingParts = oilTypeParts.subList(1, oilTypeParts.size)
                binding.dropdownOilType.setText(remainingParts.joinToString(" "))
            }
        } else {
            binding.dropdownOilType.setText(record.oilType)
        }

        binding.dropdownOilBrand.setText(record.oilBrand)
        binding.editTextOilQuantity.setText(record.oilQuantity.toString())

        // Filtros
        binding.checkboxOilFilter.isChecked = record.filterChanged

        // Establecer las observaciones originales
        binding.editTextObservations.setText(record.observations)

        // Intentar analizar las observaciones para configurar los demás checkboxes
        parseObservations(record.observations)
    }

    private fun parseObservations(observations: String) {
        val lines = observations.split("\n")

        for (line in lines) {
            when {
                line.startsWith("Filtro de aire:") -> {
                    binding.checkboxAirFilter.isChecked = true
                    binding.dropdownAirFilterNotes.setText(line.substringAfter("aire:").trim())
                }
                line.startsWith("Filtro de habitáculo:") -> {
                    binding.checkboxCabinFilter.isChecked = true
                    binding.dropdownCabinFilterNotes.setText(line.substringAfter("habitáculo:").trim())
                }
                line.startsWith("Filtro de combustible:") -> {
                    binding.checkboxFuelFilter.isChecked = true
                    binding.dropdownFuelFilterNotes.setText(line.substringAfter("combustible:").trim())
                }
                line.startsWith("Refrigerante:") -> {
                    binding.checkboxCoolant.isChecked = true
                    binding.dropdownCoolantNotes.setText(line.substringAfter("Refrigerante:").trim())
                }
                line.startsWith("Engrase:") -> {
                    binding.checkboxGrease.isChecked = true
                    binding.dropdownGreaseNotes.setText(line.substringAfter("Engrase:").trim())
                }
                line.startsWith("Aditivo") -> {
                    binding.checkboxAdditive.isChecked = true
                    if (line.contains("(") && line.contains(")")) {
                        val type = line.substringBefore("(").trim().substringAfter("Aditivo").trim()
                        binding.dropdownAdditiveType.setText(type)
                        val notes = line.substring(line.indexOf("(") + 1, line.indexOf(")"))
                        binding.dropdownAdditiveNotes.setText(notes)
                    } else {
                        binding.dropdownAdditiveType.setText(line.substringAfter("Aditivo").trim())
                    }
                }
                line.startsWith("Caja:") -> {
                    binding.checkboxGearbox.isChecked = true
                    binding.dropdownGearboxNotes.setText(line.substringAfter("Caja:").trim())
                }
                line.startsWith("Diferencial:") -> {
                    binding.checkboxDifferential.isChecked = true
                    binding.dropdownDifferentialNotes.setText(line.substringAfter("Diferencial:").trim())
                }
            }
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
            is android.widget.AutoCompleteTextView -> view.text.toString()
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

    private fun updateOilChange() {
        binding.progressBar.visibility = View.VISIBLE
        Log.d(TAG, "Actualizando registro con ID: $recordId")

        // Recolectar datos del formulario
        val oilChangeData = collectFormData()

        // Actualizar en el ViewModel
        viewModel.updateOilChangeRecord(recordId, oilChangeData)
    }

    private fun collectFormData(): OilChangeData {
        // Simplemente usaremos un valor vacío como marca del filtro de aceite
        val oilFilterBrand = ""  // Valor predeterminado

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
            oilFilterBrand = oilFilterBrand,  // Valor predeterminado
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
        // Observar carga de registro
        viewModel.recordData.observe(this) { record ->
            if (record != null) {
                Log.d(TAG, "Registro cargado con ID: ${record.id}")
                fillFormWithRecordData(record)
            } else {
                Log.e(TAG, "No se pudo cargar el registro con ID: $recordId")
                Toast.makeText(this, "No se pudo cargar el registro", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Observar estado de guardado
        viewModel.updateState.observe(this) { state ->
            when (state) {
                is OilChangeViewModel.SaveState.Loading -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.buttonRegister.isEnabled = false
                }
                is OilChangeViewModel.SaveState.Success -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonRegister.isEnabled = true
                    Toast.makeText(this, "Registro actualizado con éxito", Toast.LENGTH_SHORT).show()
                    // Finalizar actividad y volver a la pantalla anterior
                    setResult(RESULT_OK)
                    finish()
                }
                is OilChangeViewModel.SaveState.Error -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonRegister.isEnabled = true
                    Toast.makeText(this, "Error: ${state.message}", Toast.LENGTH_LONG).show()
                }
                null -> {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonRegister.isEnabled = true
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

    companion object {
        const val EXTRA_RECORD_ID = "extra_record_id"
    }
}