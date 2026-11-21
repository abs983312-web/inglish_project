package uz.inglish.app.core.network

import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import uz.inglish.app.core.network.dto.BillingProductDto
import uz.inglish.app.core.network.dto.BillingProductsResponse
import uz.inglish.app.core.network.dto.EntitlementDto
import uz.inglish.app.core.network.dto.EntitlementResponse
import uz.inglish.app.core.network.dto.RestorePurchaseItem
import uz.inglish.app.core.network.dto.RestorePurchasesRequest
import uz.inglish.app.core.network.dto.RestorePurchasesResponse
import uz.inglish.app.core.network.dto.UsageDto
import uz.inglish.app.core.network.dto.UsageResponse
import uz.inglish.app.core.network.dto.VerifyPurchaseRequest
import uz.inglish.app.core.network.dto.VerifyPurchaseResponse

class BillingApi(private val api: ApiClient) {
    suspend fun usage(): UsageDto {
        val response = api.authorizedRequest { token ->
            api.http.get("${api.base}v1/usage") { bearer(token) }
        }
        return response.bodyOrThrow<UsageResponse>().usage
    }

    suspend fun products(): List<BillingProductDto> {
        val response = api.http.get("${api.base}v1/billing/products")
        return response.bodyOrThrow<BillingProductsResponse>().products
    }

    suspend fun entitlement(): EntitlementDto {
        val response = api.authorizedRequest { token ->
            api.http.get("${api.base}v1/billing/entitlement") { bearer(token) }
        }
        return response.bodyOrThrow<EntitlementResponse>().entitlement
    }

    suspend fun verify(purchaseToken: String, productId: String? = null): VerifyPurchaseResponse {
        val response = api.authorizedRequest { token ->
            api.http.post("${api.base}v1/billing/verify") {
                bearer(token)
                contentType(ContentType.Application.Json)
                setBody(VerifyPurchaseRequest(purchaseToken = purchaseToken, productId = productId))
            }
        }
        return response.bodyOrThrow<VerifyPurchaseResponse>()
    }

    suspend fun restore(items: List<RestorePurchaseItem>): RestorePurchasesResponse {
        val response = api.authorizedRequest { token ->
            api.http.post("${api.base}v1/billing/restore") {
                bearer(token)
                contentType(ContentType.Application.Json)
                setBody(RestorePurchasesRequest(purchases = items))
            }
        }
        return response.bodyOrThrow<RestorePurchasesResponse>()
    }
}
