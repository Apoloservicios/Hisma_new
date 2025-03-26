package com.hisma.app.domain.repository

import com.hisma.app.domain.model.User
import com.hisma.app.domain.model.UserRole

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<String>
    suspend fun signUp(email: String, password: String, name: String): Result<String>
    suspend fun getCurrentUser(): User?
    suspend fun signOut()
    suspend fun updateUserRole(userId: String, role: UserRole, lubricenterId: String): Result<Unit>
}