package com.hisma.app.domain.usecase.subscription

import com.hisma.app.domain.model.Subscription
import com.hisma.app.domain.model.SubscriptionStatus
import com.hisma.app.domain.repository.SubscriptionRepository
import javax.inject.Inject

/**
 * Caso de uso para verificar el estado de suscripción de un lubricentro
 */
class CheckSubscriptionUseCase @Inject constructor(
    private val subscriptionRepository: SubscriptionRepository
) {

    sealed class SubscriptionState {
        object Valid : SubscriptionState()
        data class Expired(val message: String) : SubscriptionState()
        data class Error(val message: String) : SubscriptionState()
    }

    /**
     * Verifica si un lubricentro tiene una suscripción activa
     * @param lubricenterId ID del lubricentro
     * @return Estado de la suscripción
     */
    suspend operator fun invoke(lubricenterId: String): SubscriptionState {
        return try {
            val subscriptionResult = subscriptionRepository.getSubscriptionByLubricenterId(lubricenterId)

            if (subscriptionResult.isFailure) {
                return SubscriptionState.Error(
                    subscriptionResult.exceptionOrNull()?.message ?: "Error al verificar suscripción"
                )
            }

            val subscription = subscriptionResult.getOrNull()

            if (subscription == null) {
                return SubscriptionState.Expired(
                    "No se encontró una suscripción activa para este lubricentro."
                )
            }

            if (subscription.status != SubscriptionStatus.ACTIVE) {
                return SubscriptionState.Expired(
                    "Su suscripción ha expirado. Por favor renueve para continuar usando el servicio."
                )
            }

            // Verificar si la fecha de fin ya pasó
            val currentTime = System.currentTimeMillis()
            if (subscription.endDate < currentTime) {
                return SubscriptionState.Expired(
                    "Su período de prueba ha terminado. Por favor comuníquese con soporte o ventas al WP 2604515854 o email: ventas@hisma.com.ar"
                )
            }

            // Verificar si quedan cambios de aceite disponibles
            if (subscription.oilChangesUsed >= subscription.oilChangesLimit) {
                return SubscriptionState.Expired(
                    "Ha alcanzado el límite de cambios de aceite para su plan. Por favor actualice su suscripción."
                )
            }

            return SubscriptionState.Valid
        } catch (e: Exception) {
            SubscriptionState.Error("Error al verificar la suscripción: ${e.message}")
        }
    }
}