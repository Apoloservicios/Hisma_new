package com.hisma.app.domain.model


enum class VehicleType {
    CAR, MOTORCYCLE, TRUCK;

    companion object {
        fun fromString(value: String): VehicleType {
            return when (value.lowercase()) {
                "auto" -> CAR
                "moto" -> MOTORCYCLE
                "camión", "camion" -> TRUCK
                else -> CAR
            }
        }
    }
}