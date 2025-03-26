package com.hisma.app.data.repository

import com.hisma.app.domain.model.User
import com.hisma.app.domain.repository.AuthRepository
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor() : AuthRepository {

    override suspend fun signIn(email: String, password: String): Result<String> {
        // Simulación temporal - en una implementación real, esto se conectaría a Firebase
        return if (email.contains("@") && password.length >= 6) {
            Result.success("user_id_123")
        } else {
            Result.failure(Exception("Credenciales inválidas"))
        }
    }

    override suspend fun signUp(email: String, password: String, name: String): Result<String> {
        // Simulación temporal
        return if (email.contains("@") && password.length >= 6 && name.isNotEmpty()) {
            Result.success("new_user_id_456")
        } else {
            Result.failure(Exception("Datos inválidos"))
        }
    }

    override suspend fun getCurrentUser(): User? {
        // Simulación temporal
        return User(
            id = "user_id_123",
            email = "usuario@ejemplo.com",
            name = "Usuario Ejemplo",
            role = com.hisma.app.domain.model.UserRole.OWNER
        )
    }

    override suspend fun signOut() {
        // Simulación temporal - no hace nada por ahora
    }
}
