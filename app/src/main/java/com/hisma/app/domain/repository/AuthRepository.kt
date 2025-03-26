package com.hisma.app.domain.repository

import com.hisma.app.domain.model.User

interface AuthRepository {
    suspend fun signIn(email: String, password: String): Result<String>
    suspend fun signUp(email: String, password: String, name: String): Result<String>
    suspend fun getCurrentUser(): User?
    suspend fun signOut()
}


