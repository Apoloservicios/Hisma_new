package com.hisma.app.domain.usecase.lubricenter

import com.hisma.app.domain.model.Lubricenter
import com.hisma.app.domain.repository.LubricenterRepository
import javax.inject.Inject

class CreateLubricenterUseCase @Inject constructor(
    private val lubricenterRepository: LubricenterRepository
) {
    suspend operator fun invoke(lubricenter: Lubricenter): Result<String> {
        if (lubricenter.fantasyName.isBlank()) {
            return Result.failure(Exception("El nombre del lubricentro no puede estar vacío"))
        }

        if (lubricenter.address.isBlank()) {
            return Result.failure(Exception("La dirección del lubricentro no puede estar vacía"))
        }

        if (lubricenter.ownerId.isBlank()) {
            return Result.failure(Exception("Se requiere un propietario para el lubricentro"))
        }

        return lubricenterRepository.createLubricenter(lubricenter)
    }
}