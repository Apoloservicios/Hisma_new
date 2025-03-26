
package com.hisma.app.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.hisma.app.data.remote.FirebaseConstants
import com.hisma.app.domain.model.PlanType
import com.hisma.app.domain.model.Subscription
import com.hisma.app.domain.model.SubscriptionStatus
import com.hisma.app.domain.repository.SubscriptionRepository
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SubscriptionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore
) : SubscriptionRepository {

    override suspend fun createSubscription(
        lubricenterId: String,
        planType: PlanType,
        durationMonths: Int,
        autoRenew: Boolean
    ): Result<String> {
        return try {
            // Calcular fechas
            val startDate = System.currentTimeMillis()
            val endDate = startDate + TimeUnit.DAYS.toMillis(30L * durationMonths)

            // Calcular límites según el plan
            val oilChangesLimit = when (planType) {
                PlanType.BASIC -> 50
                PlanType.STANDARD -> 100
                PlanType.PREMIUM -> 200
            }

            val subscription = Subscription(
                lubricenterId = lubricenterId,
                planType = planType,
                status = SubscriptionStatus.ACTIVE,
                startDate = startDate,
                endDate = endDate,
                oilChangesLimit = oilChangesLimit,
                oilChangesUsed = 0,
                isAutoRenew = autoRenew
            )

            val docRef = firestore.collection(FirebaseConstants.SUBSCRIPTIONS_COLLECTION).document()
            val subscriptionId = docRef.id
            val newSubscription = subscription.copy(id = subscriptionId)

            docRef.set(newSubscription).await()

            // Actualizar el lubricentro con el ID de la suscripción
            firestore.collection(FirebaseConstants.LUBRICENTERS_COLLECTION)
                .document(lubricenterId)
                .update("subscriptionId", subscriptionId)
                .await()

            Result.success(subscriptionId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getSubscriptionByLubricenterId(lubricenterId: String): Result<Subscription?> {
        return try {
            val querySnapshot = firestore.collection(FirebaseConstants.SUBSCRIPTIONS_COLLECTION)
                .whereEqualTo("lubricenterId", lubricenterId)
                .limit(1)
                .get()
                .await()

            val subscription = querySnapshot.documents.firstOrNull()?.toObject(Subscription::class.java)
            Result.success(subscription)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renewSubscription(
        subscriptionId: String,
        planType: PlanType,
        durationMonths: Int,
        autoRenew: Boolean
    ): Result<Unit> {
        return try {
            // Obtener la suscripción actual
            val docRef = firestore.collection(FirebaseConstants.SUBSCRIPTIONS_COLLECTION)
                .document(subscriptionId)

            val subscriptionDoc = docRef.get().await()
            val subscription = subscriptionDoc.toObject(Subscription::class.java)
                ?: return Result.failure(Exception("Suscripción no encontrada"))

            // Calcular nuevas fechas y límites
            val startDate = System.currentTimeMillis()
            val endDate = startDate + TimeUnit.DAYS.toMillis(30L * durationMonths)

            val oilChangesLimit = when (planType) {
                PlanType.BASIC -> 50
                PlanType.STANDARD -> 100
                PlanType.PREMIUM -> 200
            }

            // Actualizar suscripción
            val updatedSubscription = subscription.copy(
                planType = planType,
                status = SubscriptionStatus.ACTIVE,
                startDate = startDate,
                endDate = endDate,
                oilChangesLimit = oilChangesLimit,
                oilChangesUsed = 0,
                isAutoRenew = autoRenew,
                updatedAt = System.currentTimeMillis()
            )

            docRef.set(updatedSubscription).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun incrementOilChangesUsed(subscriptionId: String): Result<Unit> {
        return try {
            // Realizar transacción para incrementar de manera segura
            firestore.runTransaction { transaction ->
                val docRef = firestore.collection(FirebaseConstants.SUBSCRIPTIONS_COLLECTION)
                    .document(subscriptionId)

                val subscriptionSnapshot = transaction.get(docRef)
                val current = subscriptionSnapshot.getLong("oilChangesUsed") ?: 0

                transaction.update(docRef, "oilChangesUsed", current + 1)
            }.await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}