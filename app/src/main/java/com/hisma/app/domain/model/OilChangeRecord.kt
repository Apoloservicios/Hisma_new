package com.hisma.app.domain.model

data class OilChangeRecord(
    val id: String = "",
    val lubricenterId: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val vehicleId: String = "",
    val vehicleBrand: String = "",
    val vehicleModel: String = "",
    val vehiclePlate: String = "",
    val vehicleYear: Int = 0,
    val oilType: String = "",
    val oilBrand: String = "",
    val oilQuantity: Double = 0.0,
    val filterChanged: Boolean = false,
    val filterBrand: String = "",
    val kilometrage: Int = 0,
    val nextChangeKm: Int = 0,
    val observations: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val createdBy: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)