package uz.inglish.app.core.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import uz.inglish.app.BuildConfig
import kotlin.coroutines.resume

/** A purchasable subscription with the Play-localised price string. */
data class PlayProduct(
    val productId: String,
    val title: String,
    val price: String,
    val details: ProductDetails,
)

/** A Play purchase that still needs server-side verification. */
data class PlayPurchase(
    val productId: String,
    val purchaseToken: String,
    val acknowledged: Boolean,
    val orderId: String?,
)

sealed interface PlayBillingEvent {
    data class Purchases(val purchases: List<PlayPurchase>) : PlayBillingEvent
    data class Error(val message: String) : PlayBillingEvent
    data object Cancelled : PlayBillingEvent
}

/**
 * Thin wrapper around Google Play Billing 7.
 *
 * The client never acknowledges purchases itself: the backend calls
 * `purchases.subscriptions.acknowledge` after it has verified the token, which
 * keeps entitlement and acknowledgement in a single place.
 *
 * When [BuildConfig.BILLING_MOCK] is true (debug builds installed outside Play)
 * the manager reports Play as unavailable so the paywall can fall back to the
 * server's mock purchase tokens.
 */
class PlayBillingManager(context: Context) {
    val isMockBuild: Boolean = BuildConfig.BILLING_MOCK

    private val _events = MutableSharedFlow<PlayBillingEvent>(extraBufferCapacity = 8)
    val events: Flow<PlayBillingEvent> = _events.asSharedFlow()

    private val purchasesUpdatedListener = PurchasesUpdatedListener { result, purchases ->
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val mapped = purchases.orEmpty().mapNotNull(::toPlayPurchase)
                if (mapped.isNotEmpty()) {
                    _events.tryEmit(PlayBillingEvent.Purchases(mapped))
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED ->
                _events.tryEmit(PlayBillingEvent.Cancelled)

            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // The user already owns the subscription; the restore path will
                // pick it up, so surface a neutral error instead of a failure.
                _events.tryEmit(PlayBillingEvent.Error("Bu obuna allaqachon faol. Tiklashni sinab ko'ring."))
            }

            else ->
                _events.tryEmit(
                    PlayBillingEvent.Error(
                        result.debugMessage.ifBlank { "Play Billing xatosi (${result.responseCode})" },
                    ),
                )
        }
    }

    private val client: BillingClient? = if (isMockBuild) {
        null
    } else {
        BillingClient.newBuilder(context.applicationContext)
            .setListener(purchasesUpdatedListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder()
                    .enableOneTimeProducts()
                    .build(),
            )
            .build()
    }

    @Volatile
    private var connected = false

    /** Connects lazily and resolves to whether Play Billing is usable. */
    suspend fun connect(): Boolean {
        val billing = client ?: return false
        if (connected && billing.isReady) return true

        return suspendCancellableCoroutine { continuation ->
            billing.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    connected = result.responseCode == BillingClient.BillingResponseCode.OK
                    if (continuation.isActive) continuation.resume(connected)
                }

                override fun onBillingServiceDisconnected() {
                    connected = false
                    if (continuation.isActive) continuation.resume(false)
                }
            })
        }
    }

    /** Queries Play product details for the given subscription IDs. */
    suspend fun queryProducts(productIds: List<String>): List<PlayProduct> {
        val billing = client ?: return emptyList()
        if (productIds.isEmpty()) return emptyList()
        if (!connect()) return emptyList()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                productIds.map { productId ->
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                },
            )
            .build()

        return suspendCancellableCoroutine { continuation ->
            billing.queryProductDetailsAsync(params) { result, details ->
                if (!continuation.isActive) return@queryProductDetailsAsync
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    continuation.resume(emptyList())
                    return@queryProductDetailsAsync
                }
                continuation.resume(
                    details.map { product ->
                        val offer = product.subscriptionOfferDetails?.firstOrNull()
                        val phase = offer?.pricingPhases?.pricingPhaseList?.firstOrNull()
                        PlayProduct(
                            productId = product.productId,
                            title = product.name,
                            price = phase?.formattedPrice ?: "",
                            details = product,
                        )
                    },
                )
            }
        }
    }

    /** Opens the Play purchase sheet. Results arrive on [events]. */
    fun launchPurchase(activity: Activity, product: PlayProduct): BillingResult? {
        val billing = client ?: return null
        val productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(product.details)
        product.details.subscriptionOfferDetails?.firstOrNull()?.offerToken?.let { offerToken ->
            productDetailsParams.setOfferToken(offerToken)
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productDetailsParams.build()))
            .build()
        return billing.launchBillingFlow(activity, params)
    }

    /** Returns all active subscription purchases for this Play account. */
    suspend fun queryActivePurchases(): List<PlayPurchase> {
        val billing = client ?: return emptyList()
        if (!connect()) return emptyList()

        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        return suspendCancellableCoroutine { continuation ->
            billing.queryPurchasesAsync(params) { result, purchases ->
                if (!continuation.isActive) return@queryPurchasesAsync
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    continuation.resume(emptyList())
                    return@queryPurchasesAsync
                }
                continuation.resume(purchases.mapNotNull(::toPlayPurchase))
            }
        }
    }

    /**
     * Optional client-side acknowledgement. The backend acknowledges too; this
     * exists only so a failed server call cannot leave a purchase unacknowledged
     * forever.
     */
    suspend fun acknowledge(purchase: PlayPurchase): Boolean {
        val billing = client ?: return false
        if (purchase.acknowledged) return true
        return suspendCancellableCoroutine { continuation ->
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billing.acknowledgePurchase(params) { result ->
                if (continuation.isActive) {
                    continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
                }
            }
        }
    }

    fun close() {
        connected = false
        client?.endConnection()
    }

    private fun toPlayPurchase(purchase: Purchase): PlayPurchase? {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return null
        val productId = purchase.products.firstOrNull() ?: return null
        return PlayPurchase(
            productId = productId,
            purchaseToken = purchase.purchaseToken,
            acknowledged = purchase.isAcknowledged,
            orderId = purchase.orderId,
        )
    }
}
