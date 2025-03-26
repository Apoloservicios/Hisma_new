package com.hisma.app.ui.profile

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.storage.FirebaseStorage
import com.hisma.app.domain.model.Lubricenter
import com.hisma.app.domain.repository.AuthRepository
import com.hisma.app.domain.repository.LubricenterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val lubricenterRepository: LubricenterRepository,
    private val storage: FirebaseStorage
) : ViewModel() {

    private val _lubricenter = MutableLiveData<Lubricenter?>()
    val lubricenter: LiveData<Lubricenter?> = _lubricenter

    private val _saveState = MutableLiveData<SaveState?>()
    val saveState: LiveData<SaveState?> = _saveState

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

    fun updateLubricenter(
        name: String,
        address: String,
        phone: String,
        email: String,
        responsible: String,
        logoUri: Uri?
    ) {
        viewModelScope.launch {
            _saveState.value = SaveState.Loading

            try {
                // Verificar que tenemos un lubricentro para actualizar
                val currentLubricenter = _lubricenter.value
                    ?: return@launch _saveState.postValue(SaveState.Error("No se encontró el lubricentro"))

                // Si hay una imagen nueva, subirla al Storage primero
                var logoUrl = currentLubricenter.logoUrl
                if (logoUri != null) {
                    logoUrl = uploadImage(logoUri)
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

    private suspend fun uploadImage(imageUri: Uri): String {
        try {
            // Crear referencia única para la imagen
            val storageRef = storage.reference
            val imageRef = storageRef.child("lubricentro_logos/${UUID.randomUUID()}.jpg")

            // Subir imagen
            val uploadTask = imageRef.putFile(imageUri).await()

            // Obtener URL de descarga
            return imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw Exception("Error al subir imagen: ${e.message}")
        }
    }

    sealed class SaveState {
        object Loading : SaveState()
        object Success : SaveState()
        data class Error(val message: String) : SaveState()
    }
}