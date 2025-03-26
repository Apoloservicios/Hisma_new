package com.hisma.app.domain.model

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val lastName: String = "",
    val lubricenterId: String = "", // ID del lubricentro al que pertenece el usuario
    val role: UserRole = UserRole.EMPLOYEE,
    val active: Boolean = true,
    val lastLogin: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class UserRole {
    SYSTEM_ADMIN,   // Administrador del sistema
    LUBRICENTER_ADMIN,  // Administrador de un lubricentro
    EMPLOYEE        // Empleado regular
}