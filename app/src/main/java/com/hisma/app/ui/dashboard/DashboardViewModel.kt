// DashboardViewModel.kt
package com.hisma.app.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hisma.app.domain.model.Lubricenter
import com.hisma.app.domain.model.User
import com.hisma.app.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _lubricenterData = MutableLiveData<Lubricenter?>()
    val lubricenterData: LiveData<Lubricenter?> = _lubricenterData

    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> = _userData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadUserData() {
        _isLoading.value = true

        val currentUser = firebaseAuth.currentUser
        if (currentUser == null) {
            _isLoading.value = false
            return
        }

        firestore.collection("users").document(currentUser.uid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Datos del usuario
                    val id = document.id
                    val name = document.getString("name") ?: ""
                    val lastName = document.getString("lastName") ?: ""
                    val email = document.getString("email") ?: ""
                    val roleStr = document.getString("role") ?: "EMPLOYEE"
                    val lubricenterId = document.getString("lubricenterId") ?: ""

                    val user = User(
                        id = id,
                        name = name,
                        lastName = lastName,
                        email = email,
                        role = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.EMPLOYEE },
                        lubricenterId = lubricenterId
                    )

                    _userData.value = user

                    // Cargar datos del lubricentro
                    if (lubricenterId.isNotEmpty()) {
                        loadLubricenterData(lubricenterId)
                    } else {
                        _isLoading.value = false
                    }
                } else {
                    _isLoading.value = false
                }
            }
            .addOnFailureListener {
                _isLoading.value = false
            }
    }

    private fun loadLubricenterData(lubricenterId: String) {
        firestore.collection("lubricenters").document(lubricenterId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Datos del lubricentro
                    val id = document.id
                    val name = document.getString("fantasyName") ?: "Lubricentro"
                    val address = document.getString("address") ?: ""
                    val phone = document.getString("phone") ?: ""
                    val email = document.getString("email") ?: ""
                    val responsible = document.getString("responsible") ?: ""

                    val lubricenter = Lubricenter(
                        id = id,
                        fantasyName = name,
                        address = address,
                        phone = phone,
                        email = email,
                        responsible = responsible
                    )

                    _lubricenterData.value = lubricenter
                }
                _isLoading.value = false
            }
            .addOnFailureListener {
                _isLoading.value = false
            }
    }
}