package com.hisma.app.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisma.app.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {


        private val _loginState = MutableLiveData<LoginState>()
        val loginState: LiveData<LoginState> = _loginState

        fun login(email: String, password: String) {
            // Validar datos
            if (email.isEmpty() || password.isEmpty()) {
                _loginState.value = LoginState.Error("Todos los campos son obligatorios")
                return
            }

            _loginState.value = LoginState.Loading

            // Simulación de proceso asíncrono
            android.os.Handler().postDelayed({
                _loginState.value = LoginState.Success
            }, 1000)
        }

        // Estados para el login
        sealed class LoginState {
            object Loading : LoginState()
            object Success : LoginState()
            data class Error(val message: String) : LoginState()
        }
    }



