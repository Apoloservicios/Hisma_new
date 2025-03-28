package com.hisma.app.ui.dashboard

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hisma.app.domain.model.Lubricenter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor() : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _lubricenterData = MutableLiveData<Lubricenter?>()
    val lubricenterData: LiveData<Lubricenter?> = _lubricenterData

    fun loadLubricenterInfo() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("DashboardViewModel", "Usuario no autenticado")
            return
        }

        Log.d("DashboardViewModel", "Cargando info para userId: $userId")

        // Primero intentamos buscar por ownerId
        loadLubricenterByOwnerId(userId)
    }

    private fun loadLubricenterByOwnerId(userId: String) {
        db.collection("lubricenters")
            .whereEqualTo("ownerId", userId)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    handleLubricenterDocuments(documents.documents[0])
                } else {
                    // Si no se encuentra como propietario, buscamos por empleado
                    loadLubricenterForEmployee(userId)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("DashboardViewModel", "Error cargando lubricentro por ownerId", exception)
                // Intentamos buscar como empleado en caso de error
                loadLubricenterForEmployee(userId)
            }
    }

    private fun loadLubricenterForEmployee(userId: String) {
        // Primero obtenemos el documento del usuario para obtener el lubricenterId
        db.collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { userDocument ->
                if (userDocument.exists()) {
                    val lubricenterId = userDocument.getString("lubricenterId")
                    if (!lubricenterId.isNullOrEmpty()) {
                        // Ahora buscamos el lubricentro con ese ID
                        db.collection("lubricenters")
                            .document(lubricenterId)
                            .get()
                            .addOnSuccessListener { lubricenterDocument ->
                                if (lubricenterDocument.exists()) {
                                    handleLubricenterDocuments(lubricenterDocument)
                                } else {
                                    Log.w("DashboardViewModel", "No se encontró el lubricentro con ID: $lubricenterId")
                                    _lubricenterData.value = null
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("DashboardViewModel", "Error cargando lubricentro por ID", exception)
                                _lubricenterData.value = null
                            }
                    } else {
                        Log.w("DashboardViewModel", "Usuario sin lubricenterId asignado")
                        _lubricenterData.value = null
                    }
                } else {
                    Log.w("DashboardViewModel", "No se encontró documento de usuario para $userId")
                    _lubricenterData.value = null
                }
            }
            .addOnFailureListener { exception ->
                Log.e("DashboardViewModel", "Error obteniendo datos de usuario", exception)
                _lubricenterData.value = null
            }
    }

    private fun handleLubricenterDocuments(document: com.google.firebase.firestore.DocumentSnapshot) {
        try {
            val lubricenter = document.toObject(Lubricenter::class.java)
            if (lubricenter != null) {
                val updatedLubricenter = lubricenter.copy(id = document.id)
                _lubricenterData.value = updatedLubricenter
                Log.d("DashboardViewModel", "Lubricentro cargado: ${updatedLubricenter.fantasyName}")
            } else {
                Log.e("DashboardViewModel", "No se pudo convertir documento a Lubricenter")
                _lubricenterData.value = null
            }
        } catch (e: Exception) {
            Log.e("DashboardViewModel", "Error procesando documento de lubricentro", e)
            _lubricenterData.value = null
        }
    }
}