package com.hisma.app.ui.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.hisma.app.domain.model.Lubricenter
import com.hisma.app.domain.repository.AuthRepository
import com.hisma.app.data.repository.LubricenterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val lubricenterRepository: LubricenterRepository
) : ViewModel() {



        private val _lubricenter = MutableLiveData<Lubricenter>()
        val lubricenter: LiveData<Lubricenter> = _lubricenter

        init {
            // Simular la carga de un lubricentro (en implementación real, esto vendría de Firebase)
            _lubricenter.value = Lubricenter(
                id = "lub1",
                name = "Lubricentro Ejemplo",
                address = "Calle Ejemplo 123",
                phone = "123-456-7890",
                email = "info@lubricentroejemplo.com",
                ownerId = "user1"
            )
        }
    }

