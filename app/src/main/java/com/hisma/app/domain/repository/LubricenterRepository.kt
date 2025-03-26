package com.hisma.app.domain.repository

import com.hisma.app.domain.model.Lubricenter

interface LubricenterRepository {
    suspend fun getLubricenterById(id: String): Result<Lubricenter>
    suspend fun getLubricentersByOwnerId(ownerId: String): Result<List<Lubricenter>>
    suspend fun createLubricenter(lubricenter: Lubricenter): Result<String>
    suspend fun updateLubricenter(lubricenter: Lubricenter): Result<Unit>
    suspend fun getLubricenterByCuit(cuit: String): Result<Lubricenter?>
}