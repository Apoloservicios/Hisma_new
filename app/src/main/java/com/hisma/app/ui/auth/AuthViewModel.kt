package com.hisma.app.ui.auth

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
class AuthViewModel @Inject constructor() : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _authState = MutableLiveData<AuthState>()
    val authState: LiveData<AuthState> = _authState

    private val _registerState = MutableLiveData<RegisterState>()
    val registerState: LiveData<RegisterState> = _registerState

    private val _lubricenterRegisterState = MutableLiveData<LubricenterRegisterState>()
    val lubricenterRegisterState: LiveData<LubricenterRegisterState> = _lubricenterRegisterState

    private val _employeeRegisterState = MutableLiveData<EmployeeRegisterState>()
    val employeeRegisterState: LiveData<EmployeeRegisterState> = _employeeRegisterState

    private val _verifyLubricenterState = MutableLiveData<VerifyLubricenterState>()
    val verifyLubricenterState: LiveData<VerifyLubricenterState> = _verifyLubricenterState

    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    private var registrationType: RegistrationType = RegistrationType.LUBRICENTER

    init {
        // Verificar si el usuario ya está autenticado
        auth.currentUser?.let {
            _authState.value = AuthState.Authenticated(it.uid)
        } ?: run {
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading

        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                _authState.value = AuthState.Authenticated(result.user?.uid ?: "")
            }
            .addOnFailureListener { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Error de autenticación")
            }
    }

    fun register(email: String, password: String) {
        _registerState.value = RegisterState.Loading

        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                _registerState.value = RegisterState.Success(result.user?.uid ?: "")
            }
            .addOnFailureListener { exception ->
                _registerState.value = RegisterState.Error(exception.message ?: "Error de registro")
            }
    }

    fun registerLubricenter(
        ownerName: String,
        ownerLastName: String,
        email: String,
        password: String,
        lubricenterName: String,
        cuit: String,
        address: String,
        phone: String
    ) {
        _lubricenterRegisterState.value = LubricenterRegisterState.Loading

        // Primero registrar el usuario en Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: ""

                // Crear el objeto Lubricenter
                val lubricenter = Lubricenter(
                    id = "",
                    fantasyName = lubricenterName,
                    cuit = cuit,
                    address = address,
                    responsible = ownerName + " " + ownerLastName,
                    email = email,
                    phone = phone,
                    ownerId = userId,
                    active = true,
                    createdAt = System.currentTimeMillis()
                )

                // Guardar en Firestore
                db.collection("lubricenters")
                    .add(lubricenter)
                    .addOnSuccessListener { documentReference ->
                        // Crear el objeto User usando tu modelo existente
                        val user = User(
                            id = userId,
                            name = ownerName,
                            lastName = ownerLastName,
                            email = email,
                            role = UserRole.LUBRICENTER_ADMIN,
                            lubricenterId = documentReference.id,
                            active = true,
                            lastLogin = System.currentTimeMillis(),
                            createdAt = System.currentTimeMillis()
                        )

                        // Guardar el usuario en Firestore
                        db.collection("users")
                            .document(userId)
                            .set(user)
                            .addOnSuccessListener {
                                _lubricenterRegisterState.value = LubricenterRegisterState.Success
                            }
                            .addOnFailureListener { exception ->
                                _lubricenterRegisterState.value =
                                    LubricenterRegisterState.Error(exception.message ?: "Error al guardar el usuario")
                            }
                    }
                    .addOnFailureListener { exception ->
                        _lubricenterRegisterState.value =
                            LubricenterRegisterState.Error(exception.message ?: "Error al guardar el lubricentro")
                    }
            }
            .addOnFailureListener { exception ->
                _lubricenterRegisterState.value =
                    LubricenterRegisterState.Error(exception.message ?: "Error de registro")
            }
    }

    fun verifyLubricenter(cuit: String) {
        _verifyLubricenterState.value = VerifyLubricenterState.Loading

        db.collection("lubricenters")
            .whereEqualTo("cuit", cuit)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    _verifyLubricenterState.value = VerifyLubricenterState.NotFound
                } else {
                    val document = documents.documents[0]
                    val lubricenter = document.toObject(Lubricenter::class.java)
                    if (lubricenter != null) {
                        // Asignar el ID del documento
                        _verifyLubricenterState.value = VerifyLubricenterState.Found(
                            lubricenter.copy(id = document.id)
                        )
                    } else {
                        _verifyLubricenterState.value = VerifyLubricenterState.Error("Error al obtener datos del lubricentro")
                    }
                }
            }
            .addOnFailureListener { exception ->
                _verifyLubricenterState.value =
                    VerifyLubricenterState.Error(exception.message ?: "Error al verificar el lubricentro")
            }
    }

    fun registerEmployee(
        name: String,
        lastName: String,
        email: String,
        password: String,
        lubricenterId: String
    ) {
        _employeeRegisterState.value = EmployeeRegisterState.Loading

        // Primero registrar el usuario en Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: ""

                // Crear el objeto User para el empleado usando tu modelo existente
                val user = User(
                    id = userId,
                    name = name,
                    lastName = lastName,
                    email = email,
                    role = UserRole.EMPLOYEE,
                    lubricenterId = lubricenterId,
                    active = true,
                    lastLogin = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )

                // Guardar el usuario en Firestore
                db.collection("users")
                    .document(userId)
                    .set(user)
                    .addOnSuccessListener {
                        _employeeRegisterState.value = EmployeeRegisterState.Success
                    }
                    .addOnFailureListener { exception ->
                        _employeeRegisterState.value =
                            EmployeeRegisterState.Error(exception.message ?: "Error al guardar el usuario")
                    }
            }
            .addOnFailureListener { exception ->
                _employeeRegisterState.value =
                    EmployeeRegisterState.Error(exception.message ?: "Error de registro")
            }
    }

    fun setRegistrationType(type: RegistrationType) {
        registrationType = type
    }

    fun navigateToLogin() {
        _navigationEvent.value = NavigationEvent.NavigateToLogin
    }

    fun navigateToRegisterLubricenter() {
        _navigationEvent.value = NavigationEvent.NavigateToRegisterLubricenter
    }

    fun navigateToRegisterEmployee() {
        _navigationEvent.value = NavigationEvent.NavigateToRegisterEmployee
    }

    // Estados para autenticación
    sealed class AuthState {
        object Unauthenticated : AuthState()
        object Loading : AuthState()
        data class Authenticated(val userId: String) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    // Estados para registro normal
    sealed class RegisterState {
        object Idle : RegisterState()
        object Loading : RegisterState()
        data class Success(val userId: String) : RegisterState()
        data class Error(val message: String) : RegisterState()
    }

    // Estados para registro de lubricentro
    sealed class LubricenterRegisterState {
        object Idle : LubricenterRegisterState()
        object Loading : LubricenterRegisterState()
        object Success : LubricenterRegisterState()
        data class Error(val message: String) : LubricenterRegisterState()
    }

    // Estados para registro de empleado
    sealed class EmployeeRegisterState {
        object Idle : EmployeeRegisterState()
        object Loading : EmployeeRegisterState()
        object Success : EmployeeRegisterState()
        data class Error(val message: String) : EmployeeRegisterState()
    }

    // Estados para verificación de lubricentro
    sealed class VerifyLubricenterState {
        object Idle : VerifyLubricenterState()
        object Loading : VerifyLubricenterState()
        object NotFound : VerifyLubricenterState()
        data class Found(val lubricenter: Lubricenter) : VerifyLubricenterState()
        data class Error(val message: String) : VerifyLubricenterState()
    }

    // Eventos de navegación
    sealed class NavigationEvent {
        object NavigateToLogin : NavigationEvent()
        object NavigateToRegisterLubricenter : NavigationEvent()
        object NavigateToRegisterEmployee : NavigationEvent()
    }

    // Tipos de registro
    enum class RegistrationType {
        LUBRICENTER, EMPLOYEE
    }
}