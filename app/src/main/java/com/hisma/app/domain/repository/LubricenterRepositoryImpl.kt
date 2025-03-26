
package com.hisma.app.data.repository

import com.hisma.app.domain.model.Lubricenter
import javax.inject.Inject

class LubricenterRepositoryImpl @Inject constructor() : LubricenterRepository {

    override suspend fun getLubricenterById(id: String): Result<Lubricenter> {
        // Simulación temporal
        return Result.success(
            Lubricenter(
                id = id,
                name = "Lubricentro Ejemplo",
                address = "Calle Ejemplo 123",
                phone = "123-456-7890",
                email = "info@lubricentroejemplo.com",
                ownerId = "user_id_123"
            )
        )
    }

    override suspend fun getLubricentersByOwnerId(ownerId: String): Result<List<Lubricenter>> {
        // Simulación temporal
        return Result.success(
            listOf(
                Lubricenter(
                    id = "lub1",
                    name = "Lubricentro Ejemplo 1",
                    address = "Calle Ejemplo 123",
                    phone = "123-456-7890",
                    email = "info@lubricentroejemplo.com",
                    ownerId = ownerId
                ),
                Lubricenter(
                    id = "lub2",
                    name = "Lubricentro Ejemplo 2",
                    address = "Calle Ejemplo 456",
                    phone = "987-654-3210",
                    email = "info@otrolubricentro.com",
                    ownerId = ownerId
                )
            )
        )
    }

    override suspend fun createLubricenter(lubricenter: Lubricenter): Result<String> {
        // Simulación temporal
        return Result.success("new_lub_id_789")
    }

    override suspend fun updateLubricenter(lubricenter: Lubricenter): Result<Unit> {
        // Simulación temporal
        return Result.success(Unit)
    }
}