package com.hisma.app.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisma.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    private val _registerState = MutableLiveData<RegisterState>()
    val registerState: LiveData<RegisterState> = _registerState

    private val _navigationEvent = MutableSharedFlow<AuthNavigationEvent>()
    val navigationEvent: SharedFlow<AuthNavigationEvent> = _navigationEvent

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

    fun navigateToRegister() {
        viewModelScope.launch {
            _navigationEvent.emit(AuthNavigationEvent.NavigateToRegister)
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

    // Estados para el registro
    sealed class RegisterState {
        object Loading : RegisterState()
        object Success : RegisterState()
        data class Error(val message: String) : RegisterState()
    }
}