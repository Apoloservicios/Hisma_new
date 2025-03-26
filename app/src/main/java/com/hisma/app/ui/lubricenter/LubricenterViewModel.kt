package com.hisma.app.ui.lubricenter

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hisma.app.domain.model.Lubricenter
import com.hisma.app.domain.repository.AuthRepository
import com.hisma.app.domain.usecase.lubricenter.CreateLubricenterUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LubricenterViewModel @Inject constructor(
    private val createLubricenterUseCase: CreateLubricenterUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _createLubricenterState = MutableLiveData<CreateLubricenterState>()
    val createLubricenterState: LiveData<CreateLubricenterState> = _createLubricenterState

    fun createLubricenter(
        fantasyName: String,
        cuit: String,
        address: String,
        phone: String,
        email: String
    ) {
        viewModelScope.launch {
            _createLubricenterState.value = CreateLubricenterState.Loading

            // Obtener el ID del usuario actual
            val currentUser = authRepository.getCurrentUser()

            if (currentUser == null) {
                _createLubricenterState.value = CreateLubricenterState.Error("Usuario no autenticado")
                return@launch
            }

            val lubricenter = Lubricenter(
                fantasyName = fantasyName,
                cuit = cuit,
                address = address,
                phone = phone,
                email = email,
                responsible = "${currentUser.name} ${currentUser.lastName}",
                ownerId = currentUser.id
            )

            createLubricenterUseCase(lubricenter)
                .onSuccess {
                    _createLubricenterState.value = CreateLubricenterState.Success(it)
                }
                .onFailure {
                    _createLubricenterState.value = CreateLubricenterState.Error(it.message ?: "Error desconocido")
                }
        }
    }

    sealed class CreateLubricenterState {
        object Loading : CreateLubricenterState()
        data class Success(val lubricenterId: String) : CreateLubricenterState()
        data class Error(val message: String) : CreateLubricenterState()
    }
}