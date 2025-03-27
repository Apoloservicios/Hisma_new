package com.hisma.app.ui.profile

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.hisma.app.domain.model.Lubricenter
import com.hisma.app.domain.repository.AuthRepository
import com.hisma.app.domain.repository.LubricenterRepository
import com.hisma.app.util.CloudinaryConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val lubricenterRepository: LubricenterRepository
) : ViewModel() {

    private val _lubricenter = MutableLiveData<Lubricenter?>()
    val lubricenter: LiveData<Lubricenter?> = _lubricenter

    private val _saveState = MutableLiveData<SaveState?>()
    val saveState: LiveData<SaveState?> = _saveState

    private var isCloudinaryInitialized = false

    init {
        loadLubricenter()
    }

    private fun loadLubricenter() {
        viewModelScope.launch {
            // Obtener el usuario actual
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                return@launch
            }

            // Obtener el lubricentro del usuario
            val lubricenterId = if (currentUser.lubricenterId.isNotEmpty()) {
                currentUser.lubricenterId
            } else {
                // Si el usuario es administrador, buscar sus lubricentros
                val lubricentersResult = lubricenterRepository.getLubricentersByOwnerId(currentUser.id)
                if (lubricentersResult.isFailure || lubricentersResult.getOrNull().isNullOrEmpty()) {
                    return@launch
                }
                lubricentersResult.getOrNull()?.firstOrNull()?.id ?: ""
            }

            // Cargar el lubricentro
            if (lubricenterId.isNotEmpty()) {
                lubricenterRepository.getLubricenterById(lubricenterId)
                    .onSuccess { lubricenter ->
                        _lubricenter.value = lubricenter
                    }
            }
        }
    }

    // Inicializar Cloudinary
    fun initializeCloudinary(context: Context) {
        if (isCloudinaryInitialized) return

        try {
            val config = CloudinaryConfig.getConfig()
            MediaManager.init(context, config)
            isCloudinaryInitialized = true
        } catch (e: Exception) {
            // MediaManager ya inicializado o error
            isCloudinaryInitialized = true
        }
    }

    fun updateLubricenter(
        name: String,
        address: String,
        phone: String,
        email: String,
        responsible: String,
        logoUri: Uri?,
        context: Context
    ) {
        viewModelScope.launch {
            _saveState.value = SaveState.Loading

            try {
                // Verificar que tenemos un lubricentro para actualizar
                val currentLubricenter = _lubricenter.value
                    ?: return@launch _saveState.postValue(SaveState.Error("No se encontró el lubricentro"))

                // Si hay una imagen nueva, subirla a Cloudinary
                var logoUrl = currentLubricenter.logoUrl
                if (logoUri != null) {
                    try {
                        initializeCloudinary(context)
                        logoUrl = uploadImageToCloudinary(logoUri, context)
                    } catch (e: Exception) {
                        _saveState.value = SaveState.Error("Error al subir imagen: ${e.message}")
                        return@launch
                    }
                }

                // Actualizar el objeto lubricentro
                val updatedLubricenter = currentLubricenter.copy(
                    fantasyName = name,
                    address = address,
                    phone = phone,
                    email = email,
                    responsible = responsible,
                    logoUrl = logoUrl
                )

                // Guardar cambios en Firestore
                lubricenterRepository.updateLubricenter(updatedLubricenter)
                    .onSuccess {
                        _lubricenter.value = updatedLubricenter
                        _saveState.value = SaveState.Success
                    }
                    .onFailure { error ->
                        _saveState.value = SaveState.Error("Error al guardar cambios: ${error.message}")
                    }

            } catch (e: Exception) {
                _saveState.value = SaveState.Error("Error inesperado: ${e.message}")
            }
        }
    }

    private suspend fun uploadImageToCloudinary(imageUri: Uri, context: Context): String = suspendCancellableCoroutine { continuation ->
        // Generar un nombre único para la imagen
        val fileName = "hisma_logo_${UUID.randomUUID()}"

        val requestId = MediaManager.get().upload(imageUri)
            .option("public_id", fileName)
            .option("folder", CloudinaryConfig.FOLDER)
            .unsigned(CloudinaryConfig.UPLOAD_PRESET)
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    // Inicio de la subida - No es necesario hacer nada aquí
                    // Ya tenemos un indicador de carga general en la UI
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    // En una implementación más avanzada, podríamos mostrar un progreso específico
                    // Por ahora, usamos el indicador general
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    // Éxito - obtener la URL
                    val secureUrl = resultData["secure_url"] as String
                    continuation.resume(secureUrl)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    // Error
                    val errorMessage = "Error al subir imagen: ${error.description}. " +
                            "Verifique su conexión a internet y vuelva a intentarlo."
                    continuation.resumeWithException(Exception(errorMessage))
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    // Reprogramado
                }
            })
            .dispatch()

        continuation.invokeOnCancellation {
            MediaManager.get().cancelRequest(requestId)
        }
    }

    sealed class SaveState {
        object Loading : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }
}