package com.hisma.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.hisma.app.data.remote.FirebaseConstants
import com.hisma.app.domain.model.User
import com.hisma.app.domain.model.UserRole
import com.hisma.app.domain.repository.AuthRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AuthRepository {

    override suspend fun signIn(email: String, password: String): Result<String> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: return Result.failure(Exception("Error al iniciar sesión"))
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signUp(email: String, password: String, name: String): Result<String> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val userId = authResult.user?.uid ?: return Result.failure(Exception("Error al crear usuario"))

            val user = User(
                id = userId,
                email = email,
                name = name,
                role = UserRole.EMPLOYEE
            )

            firestore.collection(FirebaseConstants.USERS_COLLECTION)
                .document(userId)
                .set(user)
                .await()

            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): User? {
        val firebaseUser = auth.currentUser ?: return null

        return try {
            val userDocument = firestore.collection(FirebaseConstants.USERS_COLLECTION)
                .document(firebaseUser.uid)
                .get()
                .await()

            userDocument.toObject(User::class.java)
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override suspend fun updateUserRole(userId: String, role: UserRole, lubricenterId: String): Result<Unit> {
        return try {
            firestore.collection(FirebaseConstants.USERS_COLLECTION)
                .document(userId)
                .update(
                    mapOf(
                        "role" to role,
                        "lubricenterId" to lubricenterId
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}