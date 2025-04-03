package com.hisma.app.ui.records

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.hisma.app.domain.model.OilChangeRecord
import com.hisma.app.domain.repository.AuthRepository
import com.hisma.app.domain.repository.LubricenterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "RecordsViewModel"

@HiltViewModel
class RecordsViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val lubricenterRepository: LubricenterRepository
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _records = MutableLiveData<List<OilChangeRecord>>()
    val records: LiveData<List<OilChangeRecord>> = _records

    private val _currentRecord = MutableLiveData<OilChangeRecord?>()
    val currentRecord: LiveData<OilChangeRecord?> = _currentRecord

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    // Lista original para filtrado
    private var originalRecords = mutableListOf<OilChangeRecord>()
    private var lubricenterId: String = ""

    init {
        loadRecords()
    }

    /**
     * Carga todos los registros
     */
    fun loadRecords() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            try {
                Log.d(TAG, "Iniciando carga de registros...")
                val currentUser = authRepository.getCurrentUser()
                if (currentUser == null) {
                    Log.e(TAG, "Usuario no autenticado")
                    _error.value = "Usuario no autenticado"
                    _loading.value = false
                    return@launch
                }
                Log.d(TAG, "Usuario autenticado: ${currentUser.id}")

                // Obtener el lubricentro del usuario
                lubricenterId = if (currentUser.lubricenterId.isNotEmpty()) {
                    Log.d(TAG, "Usuario tiene lubricenterId: ${currentUser.lubricenterId}")
                    currentUser.lubricenterId
                } else {
                    // Si el usuario es administrador, buscar sus lubricentros
                    Log.d(TAG, "Usuario sin lubricenterId, buscando como propietario...")
                    val lubricentersResult = lubricenterRepository.getLubricentersByOwnerId(currentUser.id)
                    if (lubricentersResult.isFailure) {
                        Log.e(TAG, "Error al obtener lubricentros: ${lubricentersResult.exceptionOrNull()?.message}")
                        _error.value = "Error al obtener lubricentros: ${lubricentersResult.exceptionOrNull()?.message}"
                        _loading.value = false
                        return@launch
                    }
                    val lubricenters = lubricentersResult.getOrNull()
                    if (lubricenters.isNullOrEmpty()) {
                        Log.e(TAG, "No se encontraron lubricentros para el usuario")
                        _error.value = "No se encontró ningún lubricentro asociado"
                        _loading.value = false
                        return@launch
                    }
                    Log.d(TAG, "Encontrados ${lubricenters.size} lubricentros, usando el primero: ${lubricenters[0].id}")
                    lubricenters[0].id
                }

                if (lubricenterId.isEmpty()) {
                    Log.e(TAG, "ID de lubricentro vacío después de la lógica de búsqueda")
                    _error.value = "No se encontró un lubricentro válido"
                    _loading.value = false
                    return@launch
                }

                // Cargar registros de la subcolección correcta
                val records = loadRecordsFromFirestore(lubricenterId)

                if (records.isEmpty()) {
                    Log.d(TAG, "No se encontraron registros para este lubricentro")
                    _error.value = "No hay registros disponibles para este lubricentro"
                } else {
                    Log.d(TAG, "Se encontraron ${records.size} registros")
                }

                originalRecords = records.toMutableList()
                _records.value = records

            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar registros", e)
                _error.value = "Error al cargar los registros: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Carga registros desde la subcolección correcta en Firestore
     */
    private suspend fun loadRecordsFromFirestore(lubricenterId: String): List<OilChangeRecord> = withContext(Dispatchers.IO) {
        val records = mutableListOf<OilChangeRecord>()

        try {
            Log.d(TAG, "Consultando subcolección oilChanges para lubricenterId: $lubricenterId")

            // Consulta a la subcolección y ordenar por fecha de creación (descendente)
            val querySnapshot = db.collection("lubricenters")
                .document(lubricenterId)
                .collection("oilChanges")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            Log.d(TAG, "Respuesta de Firestore: ${querySnapshot.documents.size} documentos")

            for (document in querySnapshot.documents) {
                val id = document.id
                try {
                    // Creamos manualmente el objeto OilChangeRecord con los datos del documento
                    val data = document.data
                    if (data != null) {
                        val record = OilChangeRecord(
                            id = id,
                            lubricenterId = data["lubricenterId"] as? String ?: lubricenterId,
                            customerId = data["customerId"] as? String ?: "",
                            customerName = data["customerName"] as? String ?: "",
                            customerPhone = data["customerPhone"] as? String ?: "",
                            vehicleId = data["vehicleId"] as? String ?: "",
                            vehicleBrand = data["vehicleBrand"] as? String ?: "",
                            vehicleModel = data["vehicleModel"] as? String ?: "",
                            vehiclePlate = data["vehiclePlate"] as? String ?: "",
                            vehicleYear = (data["vehicleYear"] as? Number)?.toInt() ?: 0,
                            oilType = data["oilType"] as? String ?: "",
                            oilBrand = data["oilBrand"] as? String ?: "",
                            oilQuantity = (data["oilQuantity"] as? Number)?.toDouble() ?: 0.0,
                            filterChanged = data["filterChanged"] as? Boolean ?: false,
                            filterBrand = data["filterBrand"] as? String ?: "",
                            kilometrage = (data["kilometrage"] as? Number)?.toInt() ?: 0,
                            nextChangeKm = (data["nextChangeKm"] as? Number)?.toInt() ?: 0,
                            observations = data["observations"] as? String ?: "",
                            createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                            createdBy = data["createdBy"] as? String ?: "",
                            updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                        )
                        Log.d(TAG, "Procesando documento ID=$id: ${record.customerName}, ${record.vehiclePlate}")
                        records.add(record)
                    } else {
                        Log.e(TAG, "Documento ID=$id no tiene datos")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error procesando documento ID=$id", e)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error cargando registros desde Firestore", e)
            throw e
        }

        Log.d(TAG, "Total de registros cargados: ${records.size}")
        records
    }

    /**
     * Busca registros según consulta
     */
    fun searchRecords(query: String) {
        viewModelScope.launch {
            _loading.value = true
            Log.d(TAG, "Buscando registros con query: '$query'")

            try {
                val filteredRecords = if (query.isBlank()) {
                    Log.d(TAG, "Query vacía, mostrando todos los registros (${originalRecords.size})")
                    originalRecords
                } else {
                    Log.d(TAG, "Filtrando ${originalRecords.size} registros")
                    originalRecords.filter {
                        it.customerName.contains(query, ignoreCase = true) ||
                                it.vehiclePlate.contains(query, ignoreCase = true) ||
                                it.vehicleBrand.contains(query, ignoreCase = true) ||
                                it.vehicleModel.contains(query, ignoreCase = true)
                    }
                }

                Log.d(TAG, "Filtrado completado. Registros resultantes: ${filteredRecords.size}")
                _records.value = filteredRecords
            } catch (e: Exception) {
                Log.e(TAG, "Error en la búsqueda", e)
                _error.value = "Error en la búsqueda: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Obtiene un registro específico por ID
     */
    fun getRecordById(recordId: String) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            Log.d(TAG, "Obteniendo registro con ID: $recordId")

            try {
                if (lubricenterId.isEmpty()) {
                    // Necesitamos obtener el lubricenterId si no lo tenemos ya
                    val currentUser = authRepository.getCurrentUser()
                    if (currentUser == null) {
                        _error.value = "Usuario no autenticado"
                        _loading.value = false
                        return@launch
                    }

                    lubricenterId = if (currentUser.lubricenterId.isNotEmpty()) {
                        currentUser.lubricenterId
                    } else {
                        val lubricenters = lubricenterRepository.getLubricentersByOwnerId(currentUser.id)
                            .getOrNull() ?: emptyList()
                        if (lubricenters.isEmpty()) {
                            _error.value = "No se encontró ningún lubricentro asociado"
                            _loading.value = false
                            return@launch
                        }
                        lubricenters[0].id
                    }
                }

                val record = getRecordFromFirestore(lubricenterId, recordId)
                if (record != null) {
                    Log.d(TAG, "Registro encontrado: ${record.id}, cliente=${record.customerName}, vehículo=${record.vehiclePlate}")
                } else {
                    Log.e(TAG, "No se encontró registro con ID: $recordId")
                }
                _currentRecord.value = record

                if (record == null) {
                    _error.value = "No se encontró el registro solicitado"
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener registro", e)
                _error.value = "Error al obtener el registro: ${e.message}"
                _currentRecord.value = null
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Obtiene un registro específico desde Firestore
     */
    private suspend fun getRecordFromFirestore(lubricenterId: String, recordId: String): OilChangeRecord? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Consultando documento con ID: $recordId en lubricenterId: $lubricenterId")
            val documentSnapshot = db.collection("lubricenters")
                .document(lubricenterId)
                .collection("oilChanges")
                .document(recordId)
                .get()
                .await()

            if (documentSnapshot.exists()) {
                Log.d(TAG, "Documento encontrado: ${documentSnapshot.data}")
                val data = documentSnapshot.data
                if (data != null) {
                    return@withContext OilChangeRecord(
                        id = recordId,
                        lubricenterId = data["lubricenterId"] as? String ?: lubricenterId,
                        customerId = data["customerId"] as? String ?: "",
                        customerName = data["customerName"] as? String ?: "",
                        customerPhone = data["customerPhone"] as? String ?: "",
                        vehicleId = data["vehicleId"] as? String ?: "",
                        vehicleBrand = data["vehicleBrand"] as? String ?: "",
                        vehicleModel = data["vehicleModel"] as? String ?: "",
                        vehiclePlate = data["vehiclePlate"] as? String ?: "",
                        vehicleYear = (data["vehicleYear"] as? Number)?.toInt() ?: 0,
                        oilType = data["oilType"] as? String ?: "",
                        oilBrand = data["oilBrand"] as? String ?: "",
                        oilQuantity = (data["oilQuantity"] as? Number)?.toDouble() ?: 0.0,
                        filterChanged = data["filterChanged"] as? Boolean ?: false,
                        filterBrand = data["filterBrand"] as? String ?: "",
                        kilometrage = (data["kilometrage"] as? Number)?.toInt() ?: 0,
                        nextChangeKm = (data["nextChangeKm"] as? Number)?.toInt() ?: 0,
                        observations = data["observations"] as? String ?: "",
                        createdAt = (data["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                        createdBy = data["createdBy"] as? String ?: "",
                        updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                }
            } else {
                Log.e(TAG, "No existe documento con ID: $recordId")
            }
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo registro de Firestore", e)
            throw e
        }
    }

    /**
     * Elimina un registro
     */
    fun deleteRecord(recordId: String) {
        viewModelScope.launch {
            _loading.value = true

            try {
                // En un caso real, eliminaríamos el registro desde un repositorio
                if (lubricenterId.isEmpty()) {
                    _error.value = "No se puede eliminar: ID de lubricentro no disponible"
                    _loading.value = false
                    return@launch
                }

                // Eliminar de Firestore
                db.collection("lubricenters")
                    .document(lubricenterId)
                    .collection("oilChanges")
                    .document(recordId)
                    .delete()
                    .await()

                // Actualizar la lista local
                val updatedList = originalRecords.filter { it.id != recordId }
                originalRecords.clear()
                originalRecords.addAll(updatedList)

                _records.value = originalRecords.toList()
                _error.value = "Registro eliminado correctamente"
            } catch (e: Exception) {
                _error.value = "Error al eliminar el registro: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }

    /**
     * Comparte detalles vía WhatsApp
     */
    fun shareViaWhatsApp(context: Context, record: OilChangeRecord) {
        try {
            val message = buildWhatsAppMessage(record)
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
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