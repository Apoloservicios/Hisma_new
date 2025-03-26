package com.hisma.app.domain.repository

import com.hisma.app.domain.model.PlanType
import com.hisma.app.domain.model.Subscription

interface SubscriptionRepository {
    suspend fun createSubscription(
        lubricenterId: String,
        planType: PlanType,
        durationMonths: Int,
        autoRenew: Boolean
    ): Result<String>

    suspend fun getSubscriptionByLubricenterId(lubricenterId: String): Result<Subscription?>

    suspend fun renewSubscription(
        subscriptionId: String,
        planType: PlanType,
        durationMonths: Int,
        autoRenew: Boolean
    ): Result<Unit>

    suspend fun incrementOilChangesUsed(subscriptionId: String): Result<Unit>
}