package com.hisma.app.ui.oilchange

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisma.app.domain.model.Lubricenter
import com.hisma.app.domain.model.OilChangeRecord
import com.hisma.app.domain.model.User
import com.hisma.app.domain.repository.AuthRepository
import com.hisma.app.domain.repository.LubricenterRepository
import com.hisma.app.util.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class OilChangeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val lubricenterRepository: LubricenterRepository
    // En una implementación real, inyectaríamos también un repositorio para los cambios de aceite
) : ViewModel() {

    private val _lubricenter = MutableLiveData<Lubricenter?>()
    val lubricenter: LiveData<Lubricenter?> = _lubricenter

    private val _saveState = MutableLiveData<SaveState>()
    val saveState: LiveData<SaveState> = _saveState

    private val _pdfFileState = MutableLiveData<File?>()
    val pdfFileState: LiveData<File?> = _pdfFileState

    // Guardamos el usuario actual para evitar llamadas suspendidas fuera de corutinas
    private var currentUser: User? = null

    init {
        loadCurrentLubricenter()
    }

    /**
     * Obtiene el lubricentro actual asociado al usuario logueado
     */
    private fun loadCurrentLubricenter() {
        viewModelScope.launch {
            try {
                currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    val lubricenterId = if (currentUser?.lubricenterId?.isNotEmpty() == true) {
                        currentUser?.lubricenterId ?: ""
                    } else {
                        // Si es admin, obtener el primer lubricentro
                        val lubricenters = lubricenterRepository.getLubricentersByOwnerId(currentUser?.id ?: "")
                            .getOrNull() ?: emptyList()
                        if (lubricenters.isNotEmpty()) lubricenters.first().id else ""
                    }

                    if (lubricenterId.isNotEmpty()) {
                        val result = lubricenterRepository.getLubricenterById(lubricenterId)
                        result.onSuccess { _lubricenter.value = it }
                    }
                }
            } catch (e: Exception) {
                Log.e("OilChangeViewModel", "Error loading lubricenter", e)
            }
        }
    }

    /**
     * Obtiene el nombre del usuario actual
     */
    fun getCurrentUserName(): String? {
        return currentUser?.name
    }

    /**
     * Guarda un registro de cambio de aceite
     */
    fun saveOilChangeRecord(oilChangeData: OilChangeData) {
        viewModelScope.launch {
            _saveState.value = SaveState.Loading

            try {
                // Crear el registro para Firestore
                val oilChangeRecord = createOilChangeRecord(oilChangeData)

                // En una implementación real, guardar en Firestore
                // Por ahora, simular guardado
                val result = simulateFirestoreSave(oilChangeRecord)

                if (result) {
                    _saveState.value = SaveState.Success
                } else {
                    _saveState.value = SaveState.Error("Error al guardar en la base de datos")
                }
            } catch (e: Exception) {
                Log.e("OilChangeViewModel", "Error al guardar registro", e)
                _saveState.value = SaveState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Genera un PDF para un registro de cambio de aceite
     */
    fun generatePdf(context: Context, oilChangeData: OilChangeData) {
        viewModelScope.launch {
            try {
                val currentLubricenter = _lubricenter.value
                if (currentLubricenter != null) {
                    val pdfFile = withContext(Dispatchers.IO) {
                        PdfGenerator.generateOilChangePdf(context, currentLubricenter, oilChangeData)
                    }
                    _pdfFileState.value = pdfFile
                } else {
                    _saveState.value = SaveState.Error("No se pudo obtener información del lubricentro")
                }
            } catch (e: Exception) {
                Log.e("OilChangeViewModel", "Error al generar PDF", e)
                _saveState.value = SaveState.Error(e.message ?: "Error al generar PDF")
            }
        }
    }

    /**
     * Crea un objeto OilChangeRecord a partir de OilChangeData
     */
    private fun createOilChangeRecord(oilChangeData: OilChangeData): OilChangeRecord {
        val currentLubricenter = _lubricenter.value
        val lubricenterId = currentLubricenter?.id ?: ""

        // En una implementación real, aquí se haría más procesamiento
        // como buscar o crear un cliente/vehículo, etc.

        return OilChangeRecord(
            id = "", // Firestore asignará el ID
            lubricenterId = lubricenterId,
            customerId = "", // En una implementación real, esto vendría de la base de datos
            customerName = oilChangeData.customerName,
            customerPhone = oilChangeData.customerPhone,
            vehicleId = "", // En una implementación real, esto vendría de la base de datos
            vehicleBrand = oilChangeData.vehicleBrand,
            vehicleModel = oilChangeData.vehicleModel,
            vehiclePlate = oilChangeData.vehiclePlate,
            vehicleYear = oilChangeData.vehicleYear,
            oilType = "${oilChangeData.oilViscosity} ${oilChangeData.oilType}",
            oilBrand = oilChangeData.oilBrand,
            oilQuantity = oilChangeData.oilQuantity.toDouble(),
            filterChanged = oilChangeData.oilFilterChanged,
            filterBrand = oilChangeData.oilFilterBrand,
            kilometrage = oilChangeData.currentKm,
            nextChangeKm = oilChangeData.nextChangeKm,
            observations = buildObservationsText(oilChangeData),
            createdAt = oilChangeData.serviceDate,
            createdBy = oilChangeData.operatorName,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Construye un texto de observaciones que incluye toda la información adicional
     */
    private fun buildObservationsText(data: OilChangeData): String {
        val sb = StringBuilder()

        // Agregar información de filtros
        if (data.airFilterChanged) {
            sb.appendLine("Filtro de aire: ${data.airFilterNotes}")
        }
        if (data.cabinFilterChanged) {
            sb.appendLine("Filtro de habitáculo: ${data.cabinFilterNotes}")
        }
        if (data.fuelFilterChanged) {
            sb.appendLine("Filtro de combustible: ${data.fuelFilterNotes}")
        }

        // Agregar información de extras
        if (data.coolantAdded) {
            sb.appendLine("Refrigerante: ${data.coolantNotes}")
        }
        if (data.greaseAdded) {
            sb.appendLine("Engrase: ${data.greaseNotes}")
        }
        if (data.additiveAdded) {
            sb.appendLine("Aditivo (${data.additiveType}): ${data.additiveNotes}")
        }
        if (data.gearboxChecked) {
            sb.appendLine("Caja: ${data.gearboxNotes}")
        }
        if (data.differentialChecked) {
            sb.appendLine("Diferencial: ${data.differentialNotes}")
        }

        // Agregar observaciones generales
        if (data.observations.isNotBlank()) {
            if (sb.isNotEmpty()) sb.appendLine()
            sb.appendLine("Observaciones adicionales:")
            sb.appendLine(data.observations)
        }

        return sb.toString()
    }

    /**
     * Simula guardar en Firestore (para demostración)
     */
    private suspend fun simulateFirestoreSave(record: OilChangeRecord): Boolean {
        return withContext(Dispatchers.IO) {
            // Simular retraso de red
            Thread.sleep(1000)
            true
        }
    }

    /**
     * Estados para el guardado
     */
    sealed class SaveState {
        object Loading : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }
}