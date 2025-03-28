package com.hisma.app.ui.oilchange

/**
 * Clase de datos mejorada que representa toda la información de un cambio de aceite
 */
data class OilChangeData(
    // Datos del cliente/vehículo
    val customerName: String,
    val customerPhone: String,
    val vehicleBrand: String,
    val vehicleModel: String,
    val vehiclePlate: String,
    val vehicleYear: Int,

    // Datos de operario y fecha
    val operatorName: String,
    val serviceDate: Long,

    // Kilometraje y periodicidad
    val currentKm: Int,
    val nextChangeKm: Int,
    val periodMonths: Int,

    // Aceite
    val oilBrand: String,
    val oilType: String,
    val oilViscosity: String,
    val oilQuantity: Float,

    // Filtros
    val oilFilterChanged: Boolean,
    val oilFilterBrand: String,
    val oilFilterNotes: String,

    val airFilterChanged: Boolean,
    val airFilterNotes: String,

    val cabinFilterChanged: Boolean,
    val cabinFilterNotes: String,

    val fuelFilterChanged: Boolean,
    val fuelFilterNotes: String,

    // Extras
    val coolantAdded: Boolean,
    val coolantNotes: String,

    val greaseAdded: Boolean,
    val greaseNotes: String,

    val additiveAdded: Boolean,
    val additiveType: String,
    val additiveNotes: String,

    val gearboxChecked: Boolean,
    val gearboxNotes: String,

    val differentialChecked: Boolean,
    val differentialNotes: String,

    // Observaciones
    val observations: String,

    // Campos que se asignan automáticamente
    val date: Long = System.currentTimeMillis(),
    val ticketId: String = generateTicketId()
)

/**
 * Genera un ID de ticket aleatorio
 */
private fun generateTicketId(): String {
    val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    return (1..10).map { chars.random() }.joinToString("")
}

/**
 * Validador de patentes argentinas
 */
object LicensePlateValidator {
    private val PATTERN_AA_123_AA = """^[A-Z]{2}\d{3}[A-Z]{2}$""".toRegex()
    private val PATTERN_ABC_123 = """^[A-Z]{3}\d{3}$""".toRegex()
    private val PATTERN_A_123_ABC = """^[A-Z]\d{3}[A-Z]{3}$""".toRegex()

    fun isValid(plate: String): Boolean {
        val trimmedPlate = plate.trim().replace("-", "").replace(" ", "").uppercase()
        return PATTERN_AA_123_AA.matches(trimmedPlate) ||
                PATTERN_ABC_123.matches(trimmedPlate) ||
                PATTERN_A_123_ABC.matches(trimmedPlate)
    }

    fun formatPlate(plate: String): String {
        val trimmedPlate = plate.trim().replace("-", "").replace(" ", "").uppercase()
        return when {
            PATTERN_AA_123_AA.matches(trimmedPlate) -> {
                // Formato: AB123CD
                trimmedPlate
            }
            PATTERN_ABC_123.matches(trimmedPlate) -> {
                // Formato: ABC123
                trimmedPlate
            }
            PATTERN_A_123_ABC.matches(trimmedPlate) -> {
                // Formato: A123BCD
                trimmedPlate
            }
            else -> trimmedPlate
        }
    }
}