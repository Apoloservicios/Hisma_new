package com.hisma.app.ui.subscription

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisma.app.domain.model.Subscription
import com.hisma.app.domain.repository.AuthRepository
import com.hisma.app.domain.repository.LubricenterRepository
import com.hisma.app.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubscriptionViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val lubricenterRepository: LubricenterRepository,
    private val subscriptionRepository: SubscriptionRepository
) : ViewModel() {

    private val _subscription = MutableLiveData<Subscription?>()
    val subscription: LiveData<Subscription?> = _subscription

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadSubscription()
    }

    private fun loadSubscription() {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null

            // Obtener el usuario actual
            val currentUser = authRepository.getCurrentUser()
            if (currentUser == null) {
                _error.value = "Usuario no autenticado"
                _loading.value = false
                return@launch
            }

            // Obtener el lubricentro del usuario
            val lubricenterId = if (currentUser.lubricenterId.isNotEmpty()) {
                currentUser.lubricenterId
            } else {
                // Si el usuario es administrador, buscar sus lubricentros
                val lubricentersResult =
                    lubricenterRepository.getLubricentersByOwnerId(currentUser.id)
                if (lubricentersResult.isFailure || lubricentersResult.getOrNull()
                        .isNullOrEmpty()
                ) {
                    _error.value = "No se encontró ningún lubricentro asociado"
                    _loading.value = false
                    return@launch
                }
                lubricentersResult.getOrNull()?.firstOrNull()?.id ?: ""
            }

            // Cargar la suscripción del lubricentro
            if (lubricenterId.isNotEmpty()) {
                subscriptionRepository.getSubscriptionByLubricenterId(lubricenterId)
                    .onSuccess { subscription ->
                        _subscription.value = subscription
                    }
                    .onFailure { error ->
                        _error.value = "Error al cargar la suscripción: ${error.message}"
                    }
            } else {
                _error.value = "No se encontró un lubricentro válido"
            }

            _loading.value = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}