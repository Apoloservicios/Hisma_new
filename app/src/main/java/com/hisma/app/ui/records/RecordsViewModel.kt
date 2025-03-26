package com.hisma.app.ui.records

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisma.app.domain.model.OilChangeRecord
import com.hisma.app.domain.repository.AuthRepository
import com.hisma.app.domain.repository.LubricenterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val lubricenterRepository: LubricenterRepository
    // private val recordsRepository: RecordsRepository // Esto sería lo ideal, pero aún no lo tenemos implementado
) : ViewModel() {

    private val _records = MutableLiveData<List<OilChangeRecord>>()
    val records: LiveData<List<OilChangeRecord>> = _records

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Lista de muestra para la demo
    private val sampleRecords = mutableListOf<OilChangeRecord>()

    init {
        // Generar datos de muestra para la demo
        generateSampleData()

        // Cargar registros
        loadRecords()
    }

    private fun generateSampleData() {
        // Solo para demostración, aquí generamos algunos registros de muestra
        val now = System.currentTimeMillis()
        val oneDayMillis = 24 * 60 * 60 * 1000L

        sampleRecords.add(
            OilChangeRecord(
                id = "1",
                lubricenterId = "123",
                customerId = "c1",
                customerName = "Juan Pérez",
                customerPhone = "26045155854",
                vehicleId = "v1",
                vehicleBrand = "Ford",
                vehicleModel = "Focus",
                vehiclePlate = "ABC123",
                vehicleYear = 2015,
                oilType = "5W30",
                oilBrand = "Mobil",
                oilQuantity = 5.0,
                filterChanged = true,
                filterBrand = "Bosch",
                kilometrage = 50000,
                nextChangeKm = 60000,
                observations = "Cambio normal",
                createdAt = now
            )
        )

        sampleRecords.add(
            OilChangeRecord(
                id = "2",
                lubricenterId = "123",
                customerId = "c2",
                customerName = "María González",
                customerPhone = "26045155854",
                vehicleId = "v2",
                vehicleBrand = "Chevrolet",
                vehicleModel = "Cruze",
                vehiclePlate = "DEF456",
                vehicleYear = 2018,
                oilType = "10W40",
                oilBrand = "Castrol",
                oilQuantity = 4.5,
                filterChanged = true,
                filterBrand = "Mann",
                kilometrage = 30000,
                nextChangeKm = 35000,
                observations = "Cliente reporta pequeña fuga",
                createdAt = now - oneDayMillis
            )
        )

        sampleRecords.add(
            OilChangeRecord(
                id = "3",
                lubricenterId = "123",
                customerId = "c3",
                customerName = "Carlos Rodríguez",
                customerPhone = "26045155854",
                vehicleId = "v3",
                vehicleBrand = "Toyota",
                vehicleModel = "Corolla",
                vehiclePlate = "GHI789",
                vehicleYear = 2020,
                oilType = "0W20",
                oilBrand = "Toyota",
                oilQuantity = 4.0,
                filterChanged = true,
                filterBrand = "Toyota",
                kilometrage = 15000,
                nextChangeKm = 25000,
                observations = "Primer servicio en el taller",
                createdAt = now - (oneDayMillis * 3)
            )
        )
    }

    fun loadRecords() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                // En un caso real, cargaríamos los datos desde un repositorio
                // En esta demo, usamos los datos de muestra
                delay(500) // Simular carga de red
                _records.value = sampleRecords.toList()
            } catch (e: Exception) {
                _error.value = "Error al cargar los registros: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun searchRecords(query: String) {
        viewModelScope.launch {
            _loading.value = true

            try {
                // Filtrar los registros según la búsqueda
                val filteredRecords = if (query.isBlank()) {
                    sampleRecords
                } else {
                    sampleRecords.filter {
                        it.customerName.contains(query, ignoreCase = true) ||
                                it.vehiclePlate.contains(query, ignoreCase = true) ||
                                it.vehicleBrand.contains(query, ignoreCase = true) ||
                                it.vehicleModel.contains(query, ignoreCase = true)
                    }
                }

                delay(300) // Simular búsqueda
                _records.value = filteredRecords
            } catch (e: Exception) {
                _error.value = "Error en la búsqueda: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun deleteRecord(recordId: String) {
        viewModelScope.launch {
            _loading.value = true

            try {
                // En un caso real, eliminaríamos el registro desde un repositorio
                // En esta demo, solo lo quitamos de la lista local
                val updatedList = sampleRecords.filter { it.id != recordId }
                sampleRecords.clear()
                sampleRecords.addAll(updatedList)

                delay(500) // Simular operación de red
                _records.value = sampleRecords.toList()
                _error.value = "Registro eliminado correctamente"
            } catch (e: Exception) {
                _error.value = "Error al eliminar el registro: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    fun generatePdf(record: OilChangeRecord) {
        // En un caso real, aquí generaríamos un PDF
        // Para esta demo, solo mostramos un mensaje
        _error.value = "Funcionalidad de generación de PDF será implementada próximamente"
    }

    fun shareViaWhatsApp(context: Context, record: OilChangeRecord) {
        try {
            val message = buildWhatsAppMessage(record)
            val intent = Intent(Intent.ACTION_VIEW)
            val url = "https://api.whatsapp.com/send?phone=${record.customerPhone.replace(" ", "")}&text=${Uri.encode(message)}"
            intent.data = Uri.parse(url)
            context.startActivity(intent)
        } catch (e: Exception) {
            _error.value = "Error al abrir WhatsApp: ${e.message}"
        }
    }

    private fun buildWhatsAppMessage(record: OilChangeRecord): String {
        return """
            *CAMBIO DE ACEITE REALIZADO*
            
            Cliente: ${record.customerName}
            Vehículo: ${record.vehicleBrand} ${record.vehicleModel} (${record.vehiclePlate})
            
            Kilometraje actual: ${record.kilometrage} km
            Próximo cambio: ${record.nextChangeKm} km
            
            Aceite: ${record.oilType} ${record.oilBrand} (${record.oilQuantity}L)
            ${if (record.filterChanged) "Filtro: ${record.filterBrand}" else "Sin cambio de filtro"}
            
            Gracias por confiar en nosotros!
        """.trimIndent()
    }

    fun clearError() {
        _error.value = null
    }
}