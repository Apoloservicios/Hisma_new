package com.hisma.app.ui.records

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hisma.app.databinding.ActivityRecordsListBinding
import com.hisma.app.domain.model.OilChange
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@AndroidEntryPoint
class RecordsListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecordsListBinding
    private val viewModel: RecordsListViewModel by viewModels()
    private lateinit var adapter: OilChangeRecordsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecordsListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupSearchFunctionality()
        setupFab()
        observeViewModel()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun setupRecyclerView() {
        adapter = OilChangeRecordsAdapter(
            onItemClick = { oilChange ->
                // Navegar a detalle del cambio (implementar según sea necesario)
                Toast.makeText(this, "Ver detalles de ${oilChange.vehiclePlate}", Toast.LENGTH_SHORT).show()
            },
            onPdfClick = { oilChange ->
                // Generar PDF (implementar según sea necesario)
                Toast.makeText(this, "Generando PDF para ${oilChange.vehiclePlate}", Toast.LENGTH_SHORT).show()
            },
            onWhatsappClick = { oilChange ->
                // Compartir por WhatsApp (implementar según sea necesario)
                Toast.makeText(this, "Compartiendo por WhatsApp ${oilChange.vehiclePlate}", Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerRecords.layoutManager = LinearLayoutManager(this)
        binding.recyclerRecords.adapter = adapter
    }

    private fun setupSearchFunctionality() {
        binding.editTextSearch.setOnEditorActionListener { _, _, _ ->
            val query = binding.editTextSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                viewModel.searchRecords(query)
            } else {
                viewModel.loadRecords()
            }
            true
        }

        // Botón de búsqueda en el layout
        val searchLayout = binding.layoutSearch
        searchLayout.setEndIconOnClickListener {
            val query = binding.editTextSearch.text.toString().trim()
            if (query.isNotEmpty()) {
                viewModel.searchRecords(query)
            } else {
                viewModel.loadRecords()
            }
        }
    }

    private fun setupFab() {
        binding.fabAddRecord.setOnClickListener {
            // Navegar a la pantalla de registro de cambio (implementar según sea necesario)
            // Ejemplo:
            // val intent = Intent(this, RegisterOilChangeActivity::class.java)
            // startActivity(intent)
        }
    }

    private fun observeViewModel() {
        viewModel.records.observe(this) { records ->
            if (records.isEmpty()) {
                binding.textEmpty.visibility = View.VISIBLE
                binding.recyclerRecords.visibility = View.GONE
            } else {
                binding.textEmpty.visibility = View.GONE
                binding.recyclerRecords.visibility = View.VISIBLE
                adapter.submitList(records)
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.errorMessage.observe(this) { message ->
            if (!message.isNullOrEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
        }

        // Cargar registros al iniciar
        viewModel.loadRecords()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}

@HiltViewModel
class RecordsListViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _records = MutableLiveData<List<OilChange>>()
    val records: LiveData<List<OilChange>> = _records

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private var currentLubricenterId: String? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    init {
        getCurrentLubricenter()
    }

    private fun getCurrentLubricenter() {
        viewModelScope.launch {
            try {
                val userId = firebaseAuth.currentUser?.uid ?: return@launch

                val userDoc = firestore.collection("users").document(userId).get().await()
                currentLubricenterId = userDoc.getString("lubricenterId")

                // Una vez que tenemos el ID del lubricentro, cargamos los registros
                loadRecords()
            } catch (e: Exception) {
                _errorMessage.value = "Error al obtener la información del usuario: ${e.message}"
            }
        }
    }

    fun loadRecords() {
        if (currentLubricenterId == null) {
            _errorMessage.value = "No se ha encontrado un lubricentro asociado"
            return
        }

        _isLoading.value = true

        firestore.collection("lubricenters")
            .document(currentLubricenterId!!)
            .collection("oilChanges")
            .orderBy("serviceDate", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                val recordsList = documents.mapNotNull { document ->
                    try {
                        documentToOilChange(document)
                    } catch (e: Exception) {
                        null
                    }
                }
                _records.value = recordsList
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Error al cargar registros: ${e.message}"
                _records.value = emptyList()
                _isLoading.value = false
            }
    }

    fun searchRecords(query: String) {
        if (currentLubricenterId == null) {
            _errorMessage.value = "No se ha encontrado un lubricentro asociado"
            return
        }

        _isLoading.value = true

        // La búsqueda se realiza de manera local primero
        // porque Firestore no permite búsquedas con LIKE o contains directamente
        firestore.collection("lubricenters")
            .document(currentLubricenterId!!)
            .collection("oilChanges")
            .get()
            .addOnSuccessListener { documents ->
                val recordsList = documents.mapNotNull { document ->
                    try {
                        documentToOilChange(document)
                    } catch (e: Exception) {
                        null
                    }
                }

                // Filtrar los registros que contienen el texto de búsqueda
                val filteredList = recordsList.filter { record ->
                    record.customerName.contains(query, ignoreCase = true) ||
                            record.vehiclePlate.contains(query, ignoreCase = true) ||
                            record.vehicleBrand.contains(query, ignoreCase = true) ||
                            record.vehicleModel.contains(query, ignoreCase = true)
                }

                _records.value = filteredList
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _errorMessage.value = "Error al buscar registros: ${e.message}"
                _records.value = emptyList()
                _isLoading.value = false
            }
    }

    private fun documentToOilChange(document: DocumentSnapshot): OilChange {
        val id = document.id
        val operatorId = document.getString("operatorId") ?: ""
        val operatorName = document.getString("operatorName") ?: ""
        val serviceDate = (document.getTimestamp("serviceDate") ?: Timestamp.now()).toDate()
        val createdAt = (document.getTimestamp("createdAt") ?: Timestamp.now()).toDate()

        val customerName = document.getString("customerName") ?: ""
        val customerPhone = document.getString("customerPhone") ?: ""
        val vehicleType = document.getString("vehicleType") ?: "Auto"
        val vehiclePlate = document.getString("vehiclePlate") ?: ""
        val vehicleBrand = document.getString("vehicleBrand") ?: ""
        val vehicleModel = document.getString("vehicleModel") ?: ""
        val vehicleYear = document.getString("vehicleYear") ?: ""

        val currentKm = document.getLong("currentKm")?.toInt() ?: 0
        val nextChangeKm = document.getLong("nextChangeKm")?.toInt() ?: 0
        val periodMonths = document.getLong("periodMonths")?.toInt() ?: 6

        val oilBrand = document.getString("oilBrand") ?: ""
        val oilType = document.getString("oilType") ?: ""
        val oilViscosity = document.getString("oilViscosity") ?: ""
        val oilQuantity = document.getDouble("oilQuantity")?.toFloat() ?: 0f

        val oilFilter = document.getBoolean("oilFilter") ?: false
        val oilFilterNotes = document.getString("oilFilterNotes") ?: ""

        val airFilter = document.getBoolean("airFilter") ?: false
        val airFilterNotes = document.getString("airFilterNotes") ?: ""

        val cabinFilter = document.getBoolean("cabinFilter") ?: false
        val cabinFilterNotes = document.getString("cabinFilterNotes") ?: ""

        val fuelFilter = document.getBoolean("fuelFilter") ?: false
        val fuelFilterNotes = document.getString("fuelFilterNotes") ?: ""

        val coolant = document.getBoolean("coolant") ?: false
        val coolantNotes = document.getString("coolantNotes") ?: ""

        val grease = document.getBoolean("grease") ?: false
        val greaseNotes = document.getString("greaseNotes") ?: ""

        val additive = document.getBoolean("additive") ?: false
        val additiveType = document.getString("additiveType") ?: ""
        val additiveNotes = document.getString("additiveNotes") ?: ""

        val gearbox = document.getBoolean("gearbox") ?: false
        val gearboxNotes = document.getString("gearboxNotes") ?: ""

        val differential = document.getBoolean("differential") ?: false
        val differentialNotes = document.getString("differentialNotes") ?: ""

        val observations = document.getString("observations") ?: ""

        return OilChange(
            id = id,
            operatorId = operatorId,
            operatorName = operatorName,
            serviceDate = serviceDate,
            createdAt = createdAt,
            customerName = customerName,
            customerPhone = customerPhone,
            vehicleType = vehicleType,
            vehiclePlate = vehiclePlate,
            vehicleBrand = vehicleBrand,
            vehicleModel = vehicleModel,
            vehicleYear = vehicleYear,
            currentKm = currentKm,
            nextChangeKm = nextChangeKm,
            periodMonths = periodMonths,
            oilBrand = oilBrand,
            oilType = oilType,
            oilViscosity = oilViscosity,
            oilQuantity = oilQuantity,
            oilFilter = oilFilter,
            oilFilterNotes = oilFilterNotes,
            airFilter = airFilter,
            airFilterNotes = airFilterNotes,
            cabinFilter = cabinFilter,
            cabinFilterNotes = cabinFilterNotes,
            fuelFilter = fuelFilter,
            fuelFilterNotes = fuelFilterNotes,
            coolant = coolant,
            coolantNotes = coolantNotes,
            grease = grease,
            greaseNotes = greaseNotes,
            additive = additive,
            additiveType = additiveType,
            additiveNotes = additiveNotes,
            gearbox = gearbox,
            gearboxNotes = gearboxNotes,
            differential = differential,
            differentialNotes = differentialNotes,
            observations = observations
        )
    }
}