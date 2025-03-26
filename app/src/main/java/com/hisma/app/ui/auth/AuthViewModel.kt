package com.hisma.app.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisma.app.domain.model.Lubricenter
import com.hisma.app.domain.model.User
import com.hisma.app.domain.model.UserRole
import com.hisma.app.domain.repository.AuthRepository
import com.hisma.app.domain.repository.LubricenterRepository
import com.hisma.app.domain.repository.SubscriptionRepository
import com.hisma.app.domain.model.PlanType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val lubricenterRepository: LubricenterRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    // Estados para login
    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    // Estados para registro
    private val _registerState = MutableLiveData<RegisterState>()
    val registerState: LiveData<RegisterState> = _registerState

    // Estados para registro de lubricentro
    private val _lubricenterRegisterState = MutableLiveData<LubricenterRegisterState>()
    val lubricenterRegisterState: LiveData<LubricenterRegisterState> = _lubricenterRegisterState

    // Estados para registro de empleado
    private val _employeeRegisterState = MutableLiveData<EmployeeRegisterState>()
    val employeeRegisterState: LiveData<EmployeeRegisterState> = _employeeRegisterState

    // Estado para verificación de lubricentro (nuevo)
    private val _verifyLubricenterState = MutableLiveData<VerifyLubricenterState>()
    val verifyLubricenterState: LiveData<VerifyLubricenterState> = _verifyLubricenterState

    // Eventos de navegación
    private val _navigationEvent = MutableSharedFlow<AuthNavigationEvent>()
    val navigationEvent: SharedFlow<AuthNavigationEvent> = _navigationEvent

    // Tipo de registro seleccionado
    private val _registrationType = MutableLiveData<RegistrationType>()
    val registrationType: LiveData<RegistrationType> = _registrationType

    // Tipo de registro
    enum class RegistrationType {
        LUBRICENTER,
        EMPLOYEE
    }

    fun setRegistrationType(type: RegistrationType) {
        _registrationType.value = type
    }

    // Función para verificar lubricentro por CUIT (nuevo)
    fun verifyLubricenter(cuit: String) {
        viewModelScope.launch {
            _verifyLubricenterState.value = VerifyLubricenterState.Loading

            lubricenterRepository.getLubricenterByCuit(cuit)
                .onSuccess { lubricenter ->
                    if (lubricenter != null) {
                        _verifyLubricenterState.value = VerifyLubricenterState.Success(lubricenter)
                    } else {
                        _verifyLubricenterState.value = VerifyLubricenterState.Error(
                            "No se encontró un lubricentro con el CUIT proporcionado"
                        )
                    }
                }
                .onFailure { error ->
                    _verifyLubricenterState.value = VerifyLubricenterState.Error(
                        error.message ?: "Error al verificar el lubricentro"
                    )
                }
        }
    }

    // Función de login
    fun login(email: String, password: String) {
        if (email.isEmpty() || password.isEmpty()) {
            _loginState.value = LoginState.Error("Todos los campos son obligatorios")
            return
        }

        _loginState.value = LoginState.Loading

        viewModelScope.launch {
            authRepository.signIn(email, password)
                .onSuccess {
                    _loginState.value = LoginState.Success
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToDashboard)
                }
                .onFailure {
                    _loginState.value = LoginState.Error(it.message ?: "Error desconocido")
                }
        }
    }

    // Registro de lubricentro
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
        if (ownerName.isEmpty() || email.isEmpty() || password.isEmpty() ||
            lubricenterName.isEmpty() || cuit.isEmpty() || address.isEmpty()) {
            _lubricenterRegisterState.value = LubricenterRegisterState.Error("Todos los campos marcados con * son obligatorios")
            return
        }

        if (password.length < 6) {
            _lubricenterRegisterState.value = LubricenterRegisterState.Error("La contraseña debe tener al menos 6 caracteres")
            return
        }

        _lubricenterRegisterState.value = LubricenterRegisterState.Loading

        viewModelScope.launch {
            // 1. Registrar usuario como LUBRICENTER_ADMIN
            val fullName = "$ownerName $ownerLastName"
            authRepository.signUp(email, password, fullName)
                .onSuccess { userId ->
                    // 2. Crear el lubricentro asociado al usuario
                    val lubricenter = Lubricenter(
                        fantasyName = lubricenterName,
                        cuit = cuit,
                        address = address,
                        phone = phone,
                        email = email,
                        responsible = fullName,
                        ownerId = userId,
                        ticketPrefix = lubricenterName.take(2).uppercase()
                    )

                    lubricenterRepository.createLubricenter(lubricenter)
                        .onSuccess { lubricenterId ->
                            // 3. Actualizar usuario con rol y ID de lubricentro
                            authRepository.updateUserRole(userId, UserRole.LUBRICENTER_ADMIN, lubricenterId)
                                .onSuccess {
                                    // 4. Crear suscripción de prueba por 15 días
                                    subscriptionRepository.createSubscription(
                                        lubricenterId = lubricenterId,
                                        planType = PlanType.BASIC,
                                        durationMonths = 1, // Un mes de prueba
                                        autoRenew = false
                                    ).onSuccess { subscriptionId ->
                                        _lubricenterRegisterState.value = LubricenterRegisterState.Success
                                        _navigationEvent.emit(AuthNavigationEvent.NavigateToDashboard)
                                    }.onFailure { error ->
                                        _lubricenterRegisterState.value = LubricenterRegisterState.Error(
                                            "Lubricentro creado pero ocurrió un error con la suscripción: ${error.message}"
                                        )
                                    }
                                }
                                .onFailure { error ->
                                    _lubricenterRegisterState.value = LubricenterRegisterState.Error(
                                        "Error al actualizar datos del usuario: ${error.message}"
                                    )
                                }
                        }
                        .onFailure { error ->
                            _lubricenterRegisterState.value = LubricenterRegisterState.Error(
                                "Error al crear lubricentro: ${error.message}"
                            )
                        }
                }
                .onFailure { error ->
                    _lubricenterRegisterState.value = LubricenterRegisterState.Error(
                        "Error en el registro: ${error.message}"
                    )
                }
        }
    }

    // Registro de empleado (modificado para usar lubricenterId)
    fun registerEmployee(
        name: String,
        lastName: String,
        email: String,
        password: String,
        lubricenterId: String // Ahora recibimos el ID directamente
    ) {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            _employeeRegisterState.value = EmployeeRegisterState.Error("Todos los campos marcados con * son obligatorios")
            return
        }

        if (password.length < 6) {
            _employeeRegisterState.value = EmployeeRegisterState.Error("La contraseña debe tener al menos 6 caracteres")
            return
        }

        _employeeRegisterState.value = EmployeeRegisterState.Loading

        viewModelScope.launch {
            // Registrar usuario como EMPLOYEE
            val fullName = "$name $lastName"
            authRepository.signUp(email, password, fullName)
                .onSuccess { userId ->
                    // Actualizar el usuario con el ID del lubricentro y el rol
                    authRepository.updateUserRole(
                        userId,
                        UserRole.EMPLOYEE,
                        lubricenterId
                    ).onSuccess {
                        _employeeRegisterState.value = EmployeeRegisterState.Success
                        _navigationEvent.emit(AuthNavigationEvent.NavigateToDashboard)
                    }.onFailure { error ->
                        _employeeRegisterState.value = EmployeeRegisterState.Error(
                            "Error al actualizar datos del usuario: ${error.message}"
                        )
                    }
                }
                .onFailure { error ->
                    _employeeRegisterState.value = EmployeeRegisterState.Error(
                        "Error en el registro: ${error.message}"
                    )
                }
        }
    }

    // Función de registro existente (podemos mantenerla por compatibilidad)
    fun register(name: String, email: String, password: String) {
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            _registerState.value = RegisterState.Error("Todos los campos son obligatorios")
            return
        }

        if (password.length < 6) {
            _registerState.value = RegisterState.Error("La contraseña debe tener al menos 6 caracteres")
            return
        }

        _registerState.value = RegisterState.Loading

        viewModelScope.launch {
            authRepository.signUp(email, password, name)
                .onSuccess {
                    _registerState.value = RegisterState.Success
                    _navigationEvent.emit(AuthNavigationEvent.NavigateToDashboard)
                }
                .onFailure {
                    _registerState.value = RegisterState.Error(it.message ?: "Error desconocido")
                }
        }
    }

    // Funciones de navegación
    fun navigateToRegister() {
        viewModelScope.launch {
            _navigationEvent.emit(AuthNavigationEvent.NavigateToRegister)
        }
    }

    fun navigateToRegisterSelection() {
        viewModelScope.launch {
            _navigationEvent.emit(AuthNavigationEvent.NavigateToRegisterSelection)
        }
    }

    fun navigateToRegisterLubricenter() {
        viewModelScope.launch {
            _navigationEvent.emit(AuthNavigationEvent.NavigateToRegisterLubricenter)
        }
    }

    fun navigateToRegisterEmployee() {
        viewModelScope.launch {
            _navigationEvent.emit(AuthNavigationEvent.NavigateToRegisterEmployee)
        }
    }

    fun navigateToLogin() {
        viewModelScope.launch {
            _navigationEvent.emit(AuthNavigationEvent.NavigateToLogin)
        }
    }

    fun navigateToForgotPassword() {
        viewModelScope.launch {
            _navigationEvent.emit(AuthNavigationEvent.NavigateToForgotPassword)
        }
    }

    // Estados para el login
    sealed class LoginState {
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
    }

    // Estados para el registro general
    sealed class RegisterState {
        object Loading : RegisterState()
        object Success : RegisterState()
        data class Error(val message: String) : RegisterState()
    }

    // Estados para el registro de lubricentro
    sealed class LubricenterRegisterState {
        object Loading : LubricenterRegisterState()
        object Success : LubricenterRegisterState()
        data class Error(val message: String) : LubricenterRegisterState()
    }

    // Estados para el registro de empleado
    sealed class EmployeeRegisterState {
        object Loading : EmployeeRegisterState()
        object Success : EmployeeRegisterState()
        data class Error(val message: String) : EmployeeRegisterState()
    }

    // Estados para la verificación de lubricentro (nuevo)
    sealed class VerifyLubricenterState {
        object Loading : VerifyLubricenterState()
        data class Success(val lubricenter: Lubricenter) : VerifyLubricenterState()
        data class Error(val message: String) : VerifyLubricenterState()
    }
}