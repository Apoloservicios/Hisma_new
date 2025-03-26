package com.hisma.app.domain.model

data class Lubricenter(
    val id: String = "",
    val fantasyName: String = "",
    val cuit: String = "",
    val address: String = "",
    val responsible: String = "",
    val location: GeoPoint? = null,
    val email: String = "",
    val phone: String = "",
    val ticketPrefix: String = "",
    val logoUrl: String = "",
    val ownerId: String = "",  // ID del usuario administrador
    val subscriptionId: String = "",  // ID de la suscripción activa
    val active: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

data class GeoPoint(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)