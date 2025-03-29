package com.hisma.app.domain.model

import java.util.Date

data class OilChange(
    val id: String = "",
    val operatorId: String = "",
    val operatorName: String = "",
    val serviceDate: Date = Date(),
    val createdAt: Date = Date(),

    // Cliente y vehículo
    val customerName: String = "",
    val customerPhone: String = "",
    val vehicleType: String = "Auto",
    val vehiclePlate: String = "",
    val vehicleBrand: String = "",
    val vehicleModel: String = "",
    val vehicleYear: String = "",

    // Kilometraje
    val currentKm: Int = 0,
    val nextChangeKm: Int = 0,
    val periodMonths: Int = 6,

    // Aceite
    val oilBrand: String = "",
    val oilType: String = "",
    val oilViscosity: String = "",
    val oilQuantity: Float = 0f,

    // Filtros
    val oilFilter: Boolean = false,
    val oilFilterNotes: String = "",

    val airFilter: Boolean = false,
    val airFilterNotes: String = "",

    val cabinFilter: Boolean = false,
    val cabinFilterNotes: String = "",

    val fuelFilter: Boolean = false,
    val fuelFilterNotes: String = "",

    // Extras
    val coolant: Boolean = false,
    val coolantNotes: String = "",

    val grease: Boolean = false,
    val greaseNotes: String = "",

    val additive: Boolean = false,
    val additiveType: String = "",
    val additiveNotes: String = "",

    val gearbox: Boolean = false,
    val gearboxNotes: String = "",

    val differential: Boolean = false,
    val differentialNotes: String = "",

    // Observaciones
    val observations: String = ""
)