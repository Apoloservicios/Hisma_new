package com.hisma.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.hisma.app.data.remote.FirebaseConstants
import com.hisma.app.domain.model.Lubricenter
import com.hisma.app.domain.repository.LubricenterRepository
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class LubricenterRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : LubricenterRepository {

    override suspend fun getLubricenterById(id: String): Result<Lubricenter> {
        return try {
            val document = firestore.collection(FirebaseConstants.LUBRICENTERS_COLLECTION)
                .document(id)
                .get()
                .await()

            val lubricenter = document.toObject(Lubricenter::class.java)
                ?: return Result.failure(Exception("Lubricentro no encontrado"))

            Result.success(lubricenter)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getLubricentersByOwnerId(ownerId: String): Result<List<Lubricenter>> {
        return try {
            val querySnapshot = firestore.collection(FirebaseConstants.LUBRICENTERS_COLLECTION)
                .whereEqualTo("ownerId", ownerId)
                .get()
                .await()

            val lubricenters = querySnapshot.documents.mapNotNull {
                it.toObject(Lubricenter::class.java)
            }

            Result.success(lubricenters)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createLubricenter(lubricenter: Lubricenter): Result<String> {
        return try {
            val documentReference = if (lubricenter.id.isEmpty()) {
                firestore.collection(FirebaseConstants.LUBRICENTERS_COLLECTION).document()
            } else {
                firestore.collection(FirebaseConstants.LUBRICENTERS_COLLECTION).document(lubricenter.id)
            }

            val id = documentReference.id
            val newLubricenter = lubricenter.copy(id = id)

            documentReference.set(newLubricenter).await()

            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLubricenter(lubricenter: Lubricenter): Result<Unit> {
        return try {
            firestore.collection(FirebaseConstants.LUBRICENTERS_COLLECTION)
                .document(lubricenter.id)
                .set(lubricenter)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    override suspend fun getLubricenterByCuit(cuit: String): Result<Lubricenter?> {
        return try {
            val querySnapshot = firestore.collection(FirebaseConstants.LUBRICENTERS_COLLECTION)
                .whereEqualTo("cuit", cuit)
                .get()
                .await()

            val lubricenter = querySnapshot.documents.firstOrNull()?.toObject(Lubricenter::class.java)
            Result.success(lubricenter)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}