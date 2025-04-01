package com.hisma.app.ui.oilchange

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.hisma.app.domain.model.Lubricenter
import com.hisma.app.domain.model.OilChangeRecord
import com.hisma.app.domain.model.User
import com.hisma.app.domain.repository.AuthRepository
import com.hisma.app.domain.repository.LubricenterRepository
import com.hisma.app.util.PdfGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

private const val TAG = "OilChangeViewModel"

@HiltViewModel
class OilChangeViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val lubricenterRepository: LubricenterRepository
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()

    private val _lubricenter = MutableLiveData<Lubricenter?>()
    val lubricenter: LiveData<Lubricenter?> = _lubricenter

    private val _saveState = MutableLiveData<SaveState>()
    val saveState: LiveData<SaveState> = _saveState

    private val _updateState = MutableLiveData<SaveState>()
    val updateState: LiveData<SaveState> = _updateState

    private val _deleteState = MutableLiveData<SaveState>()
    val deleteState: LiveData<SaveState> = _deleteState

    private val _recordData = MutableLiveData<OilChangeRecord?>()
    val recordData: LiveData<OilChangeRecord?> = _recordData

    private val _pdfFileState = MutableLiveData<PdfState>()
    val pdfFileState: LiveData<PdfState> = _pdfFileState

    // Guardamos el usuario actual para evitar llamadas suspendidas fuera de corutinas
    private var currentUser: User? = null
    private var currentLubricenterId: String = ""

    init {
        loadCurrentLubricenter()
    }

    /**
     * Obtiene el lubricentro actual asociado al usuario logueado
     */
    private fun loadCurrentLubricenter() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Cargando lubricentro actual...")
                currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    Log.d(TAG, "Usuario actual: ${currentUser?.id}")
                    val lubricenterId = if (currentUser?.lubricenterId?.isNotEmpty() == true) {
                        Log.d(TAG, "Usuario tiene lubricenterId: ${currentUser?.lubricenterId}")
                        currentUser?.lubricenterId ?: ""
                    } else {
                        // Si es admin, obtener el primer lubricentro
                        Log.d(TAG, "Buscando lubricentros por ownerId: ${currentUser?.id}")
                        val lubricenters = lubricenterRepository.getLubricentersByOwnerId(currentUser?.id ?: "")
                            .getOrNull() ?: emptyList()
                        if (lubricenters.isNotEmpty()) {
                            Log.d(TAG, "Encontrado lubricentro: ${lubricenters.first().id}")
                            lubricenters.first().id
                        } else {
                            Log.e(TAG, "No se encontraron lubricentros para este usuario")
                            ""
                        }
                    }

                    if (lubricenterId.isNotEmpty()) {
                        currentLubricenterId = lubricenterId
                        Log.d(TAG, "Obteniendo detalle de lubricentro: $lubricenterId")
                        val result = lubricenterRepository.getLubricenterById(lubricenterId)
                        result.onSuccess {
                            Log.d(TAG, "Lubricentero cargado: ${it.fantasyName}")
                            _lubricenter.value = it
                        }
                        result.onFailure {
                            Log.e(TAG, "Error al cargar lubricentro", it)
                        }
                    } else {
                        Log.e(TAG, "No se encontró un ID de lubricentro válido")
                    }
                } else {
                    Log.e(TAG, "No hay usuario autenticado")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading lubricenter", e)
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
            Log.d(TAG, "Iniciando guardado de registro...")

            try {
                // Crear el registro para Firestore
                val oilChangeRecord = createOilChangeRecord(oilChangeData)
                Log.d(TAG, "Registro creado: ${oilChangeRecord.vehiclePlate}, ${oilChangeRecord.customerName}")

                // Guardar en Firestore
                val result = saveRecordToFirestore(oilChangeRecord)

                if (result) {
                    Log.d(TAG, "Registro guardado exitosamente")
                    _saveState.value = SaveState.Success
                } else {
                    Log.e(TAG, "Error al guardar en Firestore")
                    _saveState.value = SaveState.Error("Error al guardar en la base de datos")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al guardar registro", e)
                _saveState.value = SaveState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Obtiene un registro específico para edición
     */
    fun getOilChangeRecord(recordId: String) {
        viewModelScope.launch {
            Log.d(TAG, "Obteniendo registro con ID: $recordId")
            try {
                if (currentLubricenterId.isEmpty()) {
                    loadCurrentLubricenter()
                    // Esperamos a que se cargue el lubricenterId
                    delay(500)
                }

                val lubricenterId = _lubricenter.value?.id ?: currentLubricenterId
                if (lubricenterId.isEmpty()) {
                    Log.e(TAG, "No hay lubricenterId disponible")
                    _recordData.value = null
                    return@launch
                }

                val record = getRecordFromFirestore(lubricenterId, recordId)
                if (record != null) {
                    Log.d(TAG, "Registro encontrado: ${record.customerName}, ${record.vehiclePlate}")
                } else {
                    Log.e(TAG, "No se encontró el registro con ID: $recordId")
                }
                _recordData.value = record
            } catch (e: Exception) {
                Log.e(TAG, "Error al obtener registro", e)
                _recordData.value = null
            }
        }
    }
    /**
     * Actualiza un registro existente
     */
    fun updateOilChangeRecord(recordId: String, oilChangeData: OilChangeData) {
        viewModelScope.launch {
            _updateState.value = SaveState.Loading
            Log.d(TAG, "Iniciando actualización de registro ID: $recordId")

            try {
                // Crear el registro para Firestore pero manteniendo el ID
                val oilChangeRecord = createOilChangeRecord(oilChangeData).copy(
                    id = recordId,
                    updatedAt = System.currentTimeMillis()
                )
                Log.d(TAG, "Registro actualizado: ${oilChangeRecord.vehiclePlate}, ${oilChangeRecord.customerName}")

                // Actualizar en Firestore
                val result = updateRecordInFirestore(oilChangeRecord)

                if (result) {
                    Log.d(TAG, "Registro actualizado exitosamente")
                    _updateState.value = SaveState.Success
                } else {
                    Log.e(TAG, "Error al actualizar en Firestore")
                    _updateState.value = SaveState.Error("Error al actualizar en la base de datos")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al actualizar registro", e)
                _updateState.value = SaveState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Elimina un registro existente
     */
    fun deleteOilChangeRecord(recordId: String) {
        viewModelScope.launch {
            _deleteState.value = SaveState.Loading
            Log.d(TAG, "Iniciando eliminación de registro ID: $recordId")

            try {
                val lubricenterId = _lubricenter.value?.id ?: currentLubricenterId
                if (lubricenterId.isEmpty()) {
                    Log.e(TAG, "No hay lubricenterId disponible")
                    _deleteState.value = SaveState.Error("No se pudo determinar el lubricentro")
                    return@launch
                }

                // Eliminar de Firestore
                val result = deleteRecordFromFirestore(lubricenterId, recordId)

                if (result) {
                    Log.d(TAG, "Registro eliminado exitosamente")
                    _deleteState.value = SaveState.Success
                } else {
                    Log.e(TAG, "Error al eliminar en Firestore")
                    _deleteState.value = SaveState.Error("Error al eliminar de la base de datos")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al eliminar registro", e)
                _deleteState.value = SaveState.Error(e.message ?: "Error desconocido")
            }
        }
    }

    /**
     * Genera un PDF para un registro de cambio de aceite
     */
    fun generatePdf(context: Context, recordId: String) {
        viewModelScope.launch {
            _pdfFileState.value = PdfState.Loading

            try {
                val lubricenterId = _lubricenter.value?.id ?: currentLubricenterId
                if (lubricenterId.isEmpty()) {
                    Log.e(TAG, "No hay lubricenterId disponible")
                    _pdfFileState.value = PdfState.Error("No se pudo determinar el lubricentro")
                    return@launch
                }

                val record = getRecordFromFirestore(lubricenterId, recordId)
                val currentLubricenter = _lubricenter.value

                if (record != null && currentLubricenter != null) {
                    // Convertir OilChangeRecord a OilChangeData para el generador de PDF
                    val oilChangeData = convertRecordToOilChangeData(record)

                    val pdfFile = withContext(Dispatchers.IO) {
                        PdfGenerator.generateOilChangePdf(context, currentLubricenter, oilChangeData)
                    }

                    // Crear un URI compartible para el archivo PDF
                    val pdfUri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        pdfFile
                    )

                    _pdfFileState.value = PdfState.Success(pdfFile, pdfUri)
                } else {
                    _pdfFileState.value = PdfState.Error("No se pudo obtener la información necesaria para generar el PDF")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error al generar PDF", e)
                _pdfFileState.value = PdfState.Error(e.message ?: "Error al generar PDF")
            }
        }
    }

    /**
     * Comparte un PDF vía WhatsApp
     */
    fun sharePdfViaWhatsApp(context: Context, phoneNumber: String, pdfUri: Uri) {
        try {
            val intent = Intent(Intent.ACTION_SEND)
            intent.type = "application/pdf"
            intent.putExtra(Intent.EXTRA_STREAM, pdfUri)

            // Si hay un número de teléfono, intentar abrir directamente WhatsApp con ese contacto
            if (phoneNumber.isNotEmpty()) {
                val phoneNumberFormatted = phoneNumber.replace(" ", "").replace("+", "")
                val whatsappIntent = Intent(Intent.ACTION_VIEW)
                whatsappIntent.data = Uri.parse("https://api.whatsapp.com/send?phone=$phoneNumberFormatted")
                // Verificar si WhatsApp está instalado
                if (whatsappIntent.resolveActivity(context.packageManager) != null) {
                    // Si existe, abrir WhatsApp directamente
                    context.startActivity(whatsappIntent)
                    return
                }
            }

            // Si no hay número o falla, usar el selector estándar
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val chooser = Intent.createChooser(intent, "Compartir PDF vía")
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Error al compartir PDF", e)
        }
    }

    /**
     * Comparte detalles por WhatsApp
     */
    fun shareViaWhatsApp(context: Context, record: OilChangeRecord) {
        try {
            val message = buildWhatsAppMessage(record)
            val intent = Intent(Intent.ACTION_VIEW)
            val phoneNumber = record.customerPhone.replace(" ", "").replace("+", "")
            val url = "https://api.whatsapp.com/send?phone=$phoneNumber&text=${Uri.encode(message)}"
            intent.data = Uri.parse(url)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error al abrir WhatsApp", e)
        }
    }

    /**
     * Construye un mensaje para WhatsApp
     */
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

    /**
     * Convierte un OilChangeRecord a OilChangeData para el generador de PDF
     */
    private fun convertRecordToOilChangeData(record: OilChangeRecord): OilChangeData {
        // Extraer información del aceite
        val oilTypeParts = record.oilType.split(" ")
        val oilViscosity = if (oilTypeParts.isNotEmpty()) oilTypeParts[0] else ""
        val oilType = if (oilTypeParts.size > 1) oilTypeParts.subList(1, oilTypeParts.size).joinToString(" ") else record.oilType

        // Parsear observaciones para extraer información de filtros y extras
        val parsedInfo = parseObservationsText(record.observations)

        return OilChangeData(
            customerName = record.customerName,
            customerPhone = record.customerPhone,
            vehicleBrand = record.vehicleBrand,
            vehicleModel = record.vehicleModel,
            vehiclePlate = record.vehiclePlate,
            vehicleYear = record.vehicleYear,
            operatorName = record.createdBy,
            serviceDate = record.createdAt,
            currentKm = record.kilometrage,
            nextChangeKm = record.nextChangeKm,
            periodMonths = 6, // Valor por defecto
            oilBrand = record.oilBrand,
            oilType = oilType,
            oilViscosity = oilViscosity,
            oilQuantity = record.oilQuantity.toFloat(),

            // Filtros
            oilFilterChanged = record.filterChanged,
            oilFilterBrand = record.filterBrand,
            oilFilterNotes = parsedInfo.oilFilterNotes,

            airFilterChanged = parsedInfo.airFilterChanged,
            airFilterNotes = parsedInfo.airFilterNotes,

            cabinFilterChanged = parsedInfo.cabinFilterChanged,
            cabinFilterNotes = parsedInfo.cabinFilterNotes,

            fuelFilterChanged = parsedInfo.fuelFilterChanged,
            fuelFilterNotes = parsedInfo.fuelFilterNotes,

            // Extras
            coolantAdded = parsedInfo.coolantAdded,
            coolantNotes = parsedInfo.coolantNotes,

            greaseAdded = parsedInfo.greaseAdded,
            greaseNotes = parsedInfo.greaseNotes,

            additiveAdded = parsedInfo.additiveAdded,
            additiveType = parsedInfo.additiveType,
            additiveNotes = parsedInfo.additiveNotes,

            gearboxChecked = parsedInfo.gearboxChecked,
            gearboxNotes = parsedInfo.gearboxNotes,

            differentialChecked = parsedInfo.differentialChecked,
            differentialNotes = parsedInfo.differentialNotes,

            observations = record.observations
        )
    }
    /**
     * Clase auxiliar para almacenar la información parseada de las observaciones
     */
    private data class ParsedInfo(
        val airFilterChanged: Boolean = false,
        val airFilterNotes: String = "N/A",
        val cabinFilterChanged: Boolean = false,
        val cabinFilterNotes: String = "N/A",
        val fuelFilterChanged: Boolean = false,
        val fuelFilterNotes: String = "N/A",
        val oilFilterNotes: String = "N/A",
        val coolantAdded: Boolean = false,
        val coolantNotes: String = "N/A",
        val greaseAdded: Boolean = false,
        val greaseNotes: String = "N/A",
        val additiveAdded: Boolean = false,
        val additiveType: String = "",
        val additiveNotes: String = "N/A",
        val gearboxChecked: Boolean = false,
        val gearboxNotes: String = "N/A",
        val differentialChecked: Boolean = false,
        val differentialNotes: String = "N/A"
    )

    /**
     * Parsea el campo de observaciones para extraer información sobre filtros y extras
     */
    private fun parseObservationsText(observations: String): ParsedInfo {
        val result = ParsedInfo()
        val lines = observations.split("\n")

        for (line in lines) {
            when {
                line.startsWith("Filtro de aire:") -> {
                    val modified = result.copy(
                        airFilterChanged = true,
                        airFilterNotes = line.substringAfter("aire:").trim()
                    )
                    return modified
                }
                line.startsWith("Filtro de habitáculo:") -> {
                    val modified = result.copy(
                        cabinFilterChanged = true,
                        cabinFilterNotes = line.substringAfter("habitáculo:").trim()
                    )
                    return modified
                }
                line.startsWith("Filtro de combustible:") -> {
                    val modified = result.copy(
                        fuelFilterChanged = true,
                        fuelFilterNotes = line.substringAfter("combustible:").trim()
                    )
                    return modified
                }
                line.startsWith("Refrigerante:") -> {
                    val modified = result.copy(
                        coolantAdded = true,
                        coolantNotes = line.substringAfter("Refrigerante:").trim()
                    )
                    return modified
                }
                line.startsWith("Engrase:") -> {
                    val modified = result.copy(
                        greaseAdded = true,
                        greaseNotes = line.substringAfter("Engrase:").trim()
                    )
                    return modified
                }
                line.startsWith("Aditivo") -> {
                    if (line.contains("(") && line.contains(")")) {
                        val type = line.substringBefore("(").trim().substringAfter("Aditivo").trim()
                        val notes = line.substring(line.indexOf("(") + 1, line.indexOf(")"))
                        val modified = result.copy(
                            additiveAdded = true,
                            additiveType = type,
                            additiveNotes = notes
                        )
                        return modified
                    } else {
                        val modified = result.copy(
                            additiveAdded = true,
                            additiveType = line.substringAfter("Aditivo").trim()
                        )
                        return modified
                    }
                }
                line.startsWith("Caja:") -> {
                    val modified = result.copy(
                        gearboxChecked = true,
                        gearboxNotes = line.substringAfter("Caja:").trim()
                    )
                    return modified
                }
                line.startsWith("Diferencial:") -> {
                    val modified = result.copy(
                        differentialChecked = true,
                        differentialNotes = line.substringAfter("Diferencial:").trim()
                    )
                    return modified
                }
            }
        }

        return result
    }

    /**
     * Crea un objeto OilChangeRecord a partir de OilChangeData
     */
    private fun createOilChangeRecord(oilChangeData: OilChangeData): OilChangeRecord {
        val currentLubricenter = _lubricenter.value
        val lubricenterId = currentLubricenter?.id ?: currentLubricenterId
        if (lubricenterId.isEmpty()) {
            Log.e(TAG, "No hay lubricenterId disponible")
        } else {
            Log.d(TAG, "Usando lubricenterId: $lubricenterId")
        }

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
            sb.appendLine("Aditivo ${data.additiveType}: (${data.additiveNotes})")
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
     * Guarda un registro en Firestore
     */
    private suspend fun saveRecordToFirestore(record: OilChangeRecord): Boolean {
        return try {
            val lubricenterId = record.lubricenterId
            if (lubricenterId.isEmpty()) {
                Log.e(TAG, "lubricenterId vacío, no se puede guardar el registro")
                return false
            }

            Log.d(TAG, "Guardando registro en subcolección oilChanges de lubricenterId: $lubricenterId")

            // Usar la ruta correcta a la subcolección
            val docRef = db.collection("lubricenters")
                .document(lubricenterId)
                .collection("oilChanges")
                .document()

            val recordWithId = record.copy(id = docRef.id)
            docRef.set(recordWithId).await()

            Log.d(TAG, "Registro guardado correctamente con ID: ${docRef.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando en Firestore", e)
            false
        }
    }

    /**
     * Obtiene un registro desde Firestore
     */
    private suspend fun getRecordFromFirestore(lubricenterId: String, recordId: String): OilChangeRecord? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Consultando documento con ID: $recordId en lubricenterId: $lubricenterId")

            val doc = db.collection("lubricenters")
                .document(lubricenterId)
                .collection("oilChanges")
                .document(recordId)
                .get()
                .await()

            if (doc.exists()) {
                Log.d(TAG, "Documento encontrado: ${doc.data}")
                // Implementación correcta para convertir documento a OilChangeRecord
                val data = doc.data
                if (data != null) {
                    OilChangeRecord(
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
                } else {
                    Log.e(TAG, "Documento sin datos")
                    null
                }
            } else {
                Log.e(TAG, "No existe documento con ID: $recordId")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obteniendo registro de Firestore", e)
            throw e
        }
    }

    /**
     * Actualiza un registro en Firestore
     */
    private suspend fun updateRecordInFirestore(record: OilChangeRecord): Boolean {
        return try {
            val lubricenterId = record.lubricenterId
            val recordId = record.id

            if (lubricenterId.isEmpty()) {
                Log.e(TAG, "lubricenterId vacío, no se puede actualizar el registro")
                return false
            }

            if (recordId.isEmpty()) {
                Log.e(TAG, "recordId vacío, no se puede actualizar el registro")
                return false
            }

            Log.d(TAG, "Actualizando registro en subcolección oilChanges, lubricenterId: $lubricenterId, recordId: $recordId")

            db.collection("lubricenters")
                .document(lubricenterId)
                .collection("oilChanges")
                .document(recordId)
                .set(record)
                .await()

            Log.d(TAG, "Registro actualizado correctamente")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error actualizando en Firestore", e)
            false
        }
    }

    /**
     * Elimina un registro de Firestore
     */
    private suspend fun deleteRecordFromFirestore(lubricenterId: String, recordId: String): Boolean {
        return try {
            if (lubricenterId.isEmpty()) {
                Log.e(TAG, "lubricenterId vacío, no se puede eliminar el registro")
                return false
            }

            if (recordId.isEmpty()) {
                Log.e(TAG, "recordId vacío, no se puede eliminar el registro")
                return false
            }

            Log.d(TAG, "Eliminando registro de subcolección oilChanges, lubricenterId: $lubricenterId, recordId: $recordId")

            db.collection("lubricenters")
                .document(lubricenterId)
                .collection("oilChanges")
                .document(recordId)
                .delete()
                .await()

            Log.d(TAG, "Registro eliminado correctamente")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error eliminando de Firestore", e)
            false
        }
    }

    /**
     * Estados para el guardado, actualización y eliminación
     */
    sealed class SaveState {
        object Loading : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }

    /**
     * Estados para la generación de PDF
     */
    sealed class PdfState {
        object Loading : PdfState()
        data class Success(val file: File, val uri: Uri) : PdfState()
        data class Error(val message: String) : PdfState()
    }
}