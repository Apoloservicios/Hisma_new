package com.hisma.app.domain.model

data class Subscription(
    val id: String = "",
    val lubricenterId: String = "",
    val planType: PlanType = PlanType.BASIC,
    val status: SubscriptionStatus = SubscriptionStatus.INACTIVE,
    val startDate: Long = 0,
    val endDate: Long = 0,
    val oilChangesLimit: Int = 0,
    val oilChangesUsed: Int = 0,
    val isAutoRenew: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class PlanType {
    BASIC, STANDARD, PREMIUM
}

enum class SubscriptionStatus {
    ACTIVE, INACTIVE, EXPIRED, CANCELLED
}