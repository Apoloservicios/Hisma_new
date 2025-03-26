package com.hisma.app.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisma.app.domain.model.Lubricenter
import com.hisma.app.domain.model.User
import com.hisma.app.domain.repository.AuthRepository
import com.hisma.app.domain.repository.LubricenterRepository
import com.hisma.app.domain.usecase.subscription.CheckSubscriptionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val lubricenterRepository: LubricenterRepository,
    private val checkSubscriptionUseCase: CheckSubscriptionUseCase
) : ViewModel() {

    private val _lubricenter = MutableLiveData<Lubricenter>()
    val lubricenter: LiveData<Lubricenter> = _lubricenter

    private val _currentUser = MutableLiveData<User?>()
    val currentUser: LiveData<User?> = _currentUser

    private val _subscriptionState = MutableLiveData<CheckSubscriptionUseCase.SubscriptionState>()
    val subscriptionState: LiveData<CheckSubscriptionUseCase.SubscriptionState> = _subscriptionState

    private val _navigationEvent = MutableSharedFlow<DashboardNavigationEvent>()
    val navigationEvent: SharedFlow<DashboardNavigationEvent> = _navigationEvent

    init {
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            _currentUser.value = user

            // Cargar lubricentro asociado al usuario
            user?.let {
                if (user.lubricenterId.isNotEmpty()) {
                    loadLubricenterById(user.lubricenterId)
                } else {
                    // Si el usuario es un administrador, cargar sus lubricentros
                    loadLubricentersByOwnerId(user.id)
                }
            }
        }
    }

    private fun loadLubricenterById(lubricenterId: String) {
        viewModelScope.launch {
            lubricenterRepository.getLubricenterById(lubricenterId)
                .onSuccess { lubricenter ->
                    _lubricenter.value = lubricenter
                    checkSubscription(lubricenter.id)
                }
                .onFailure {
                    // Manejar error
                }
        }
    }

    private fun loadLubricentersByOwnerId(ownerId: String) {
        viewModelScope.launch {
            lubricenterRepository.getLubricentersByOwnerId(ownerId)
                .onSuccess { lubricenters ->
                    if (lubricenters.isNotEmpty()) {
                        _lubricenter.value = lubricenters.first()
                        checkSubscription(lubricenters.first().id)
                    }
                }
                .onFailure {
                    // Manejar error
                }
        }
    }

    fun checkSubscription(lubricenterId: String) {
        viewModelScope.launch {
            val state = checkSubscriptionUseCase(lubricenterId)
            _subscriptionState.value = state

            if (state !is CheckSubscriptionUseCase.SubscriptionState.Valid) {
                // Si no es válida, navegar a pantalla de suscripción expirada
                val message = when (state) {
                    is CheckSubscriptionUseCase.SubscriptionState.Expired -> state.message
                    is CheckSubscriptionUseCase.SubscriptionState.Error -> "Error al verificar su suscripción. Por favor contacte a soporte."
                    else -> "Su suscripción ha expirado."
                }
                _navigationEvent.emit(DashboardNavigationEvent.NavigateToSubscriptionExpired(message))
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
            _navigationEvent.emit(DashboardNavigationEvent.NavigateToLogin)
        }
    }

    sealed class DashboardNavigationEvent {
        data class NavigateToSubscriptionExpired(val message: String) : DashboardNavigationEvent()
        object NavigateToLogin : DashboardNavigationEvent()
    }
}