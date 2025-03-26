package com.hisma.app.domain.model

data class User(
    val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: UserRole = UserRole.EMPLOYEE
)

enum class UserRole {
    ADMIN, OWNER, MANAGER, EMPLOYEE
}


