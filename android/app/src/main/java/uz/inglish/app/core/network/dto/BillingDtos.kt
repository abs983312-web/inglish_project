package uz.inglish.app.core.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class UsageResponse(
    val usage: UsageDto,
)

@Serializable
data class UsageDto(
    val plan: String = "FREE",
    val speaking: UsageBucketDto = UsageBucketDto(),
    val writing: UsageBucketDto = UsageBucketDto(),
)

@Serializable
data class UsageBucketDto(
    val used: Int = 0,
    val limit: Int = 0,
    val remaining: Int = 0,
    val resetsAt: String = "",
)

@Serializable
data class BillingProductsResponse(
    val products: List<BillingProductDto> = emptyList(),
    val packageName: String = "",
)

@Serializable
data class BillingProductDto(
    val productId: String,
    val plan: String = "PRO",
    val title: String = "",
    val description: String = "",
    val days: Int = 30,
)

@Serializable
data class EntitlementResponse(
    val entitlement: EntitlementDto,
)

@Serializable
data class EntitlementDto(
    val plan: String = "FREE",
    val status: String = "ACTIVE",
    val startedAt: String? = null,
    val expiresAt: String? = null,
    val provider: String? = null,
    val autoRenewing: Boolean = false,
    val limits: PlanLimitsDto = PlanLimitsDto(),
)

@Serializable
data class PlanLimitsDto(
    val speakingPerDay: Int = 0,
    val writingPerMonth: Int = 0,
)

@Serializable
data class VerifyPurchaseRequest(
    val purchaseToken: String,
    val productId: String? = null,
    val packageName: String? = null,
)

@Serializable
data class VerifyPurchaseResponse(
    val verified: Boolean = false,
    val purchase: VerifiedPurchaseDto,
    val entitlement: EntitlementDto,
)

@Serializable
data class VerifiedPurchaseDto(
    val productId: String = "",
    val purchaseToken: String = "",
    val orderId: String? = null,
    val purchaseTime: String? = null,
    val expiryTime: String? = null,
    val autoRenewing: Boolean = false,
    val mock: Boolean = false,
)

@Serializable
data class RestorePurchasesRequest(
    val purchases: List<RestorePurchaseItem>,
)

@Serializable
data class RestorePurchaseItem(
    val purchaseToken: String,
    val productId: String? = null,
)

@Serializable
data class RestorePurchasesResponse(
    val restored: List<RestoredPurchaseDto> = emptyList(),
    val skipped: List<SkippedPurchaseDto> = emptyList(),
    val entitlement: EntitlementDto,
)

@Serializable
data class RestoredPurchaseDto(
    val purchaseId: String = "",
    val plan: String = "PRO",
    val status: String = "ACTIVE",
    val expiresAt: String? = null,
)

@Serializable
data class SkippedPurchaseDto(
    val purchaseToken: String = "",
    val reason: String = "",
)
