package uz.inglish.app.feature.billing

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.android.billingclient.api.BillingClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uz.inglish.app.core.AppContainer
import uz.inglish.app.core.billing.PlayBillingEvent
import uz.inglish.app.core.billing.PlayBillingManager
import uz.inglish.app.core.billing.PlayProduct
import uz.inglish.app.core.network.BillingApi
import uz.inglish.app.core.network.dto.BillingProductDto
import uz.inglish.app.core.network.dto.EntitlementDto
import uz.inglish.app.core.network.dto.RestorePurchaseItem
import uz.inglish.app.core.network.userMessage

data class PaywallUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val message: String? = null,
    val entitlement: EntitlementDto? = null,
    val catalog: List<BillingProductDto> = emptyList(),
    val playProducts: Map<String, PlayProduct> = emptyMap(),
    val playAvailable: Boolean = false,
    val mockBuild: Boolean = false,
    val purchasingProductId: String? = null,
    val restoring: Boolean = false,
)

class PaywallViewModel(
    private val billingApi: BillingApi,
    private val playBilling: PlayBillingManager,
) : ViewModel() {
    private val _state = MutableStateFlow(
        PaywallUiState(mockBuild = playBilling.isMockBuild),
    )
    val state: StateFlow<PaywallUiState> = _state.asStateFlow()

    init {
        observePlayEvents()
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try {
                val entitlement = billingApi.entitlement()
                val catalog = billingApi.products()
                _state.update { it.copy(entitlement = entitlement, catalog = catalog) }

                val available = playBilling.connect()
                val playProducts = if (available) {
                    playBilling.queryProducts(catalog.map { it.productId })
                        .associateBy { it.productId }
                } else {
                    emptyMap()
                }

                _state.update {
                    it.copy(
                        loading = false,
                        playAvailable = available && playProducts.isNotEmpty(),
                        playProducts = playProducts,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(loading = false, error = error.userMessage()) }
            }
        }
    }

    fun purchase(activity: Activity, product: BillingProductDto) {
        if (_state.value.purchasingProductId != null) return
        _state.update { it.copy(purchasingProductId = product.productId, error = null, message = null) }

        val playProduct = _state.value.playProducts[product.productId]
        if (_state.value.playAvailable && playProduct != null) {
            val result = playBilling.launchPurchase(activity, playProduct)
            if (result == null) {
                _state.update {
                    it.copy(
                        purchasingProductId = null,
                        error = "Play Billing mavjud emas. Qayta urinib ko'ring.",
                    )
                }
            } else if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                // launchBillingFlow failed before the sheet opened, so the
                // purchasesUpdatedListener will not fire.
                _state.update {
                    it.copy(
                        purchasingProductId = null,
                        error = result.debugMessage.ifBlank {
                            "Play Billing xatosi (${result.responseCode})"
                        },
                    )
                }
            }
            // A successful launch reports through observePlayEvents().
            return
        }

        // Sideloaded debug builds and Play-less devices use server mock tokens.
        val nonce = java.util.UUID.randomUUID().toString().take(8)
        verifyToken(
            purchaseToken = "mock:${product.productId}:${product.days}:$nonce",
            productId = product.productId,
        )
    }

    fun restore() {
        if (_state.value.restoring) return
        _state.update { it.copy(restoring = true, error = null, message = null) }
        viewModelScope.launch {
            try {
                val purchases = playBilling.queryActivePurchases()
                if (purchases.isEmpty()) {
                    val entitlement = billingApi.entitlement()
                    _state.update {
                        it.copy(
                            restoring = false,
                            entitlement = entitlement,
                            message = "Tiklanadigan xarid topilmadi.",
                        )
                    }
                    return@launch
                }

                val response = billingApi.restore(
                    purchases.map {
                        RestorePurchaseItem(
                            purchaseToken = it.purchaseToken,
                            productId = it.productId,
                        )
                    },
                )
                _state.update {
                    it.copy(
                        restoring = false,
                        entitlement = response.entitlement,
                        message = if (response.restored.isNotEmpty()) {
                            "Obuna tiklandi."
                        } else {
                            "Tiklanadigan faol obuna topilmadi."
                        },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update { it.copy(restoring = false, error = error.userMessage()) }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(error = null, message = null) }
    }

    private fun observePlayEvents() {
        viewModelScope.launch {
            playBilling.events.collect { event ->
                when (event) {
                    is PlayBillingEvent.Purchases -> {
                        val purchase = event.purchases.firstOrNull()
                        if (purchase == null) {
                            _state.update { it.copy(purchasingProductId = null) }
                        } else {
                            verifyToken(purchase.purchaseToken, purchase.productId)
                        }
                    }

                    is PlayBillingEvent.Error ->
                        _state.update {
                            it.copy(purchasingProductId = null, error = event.message)
                        }

                    PlayBillingEvent.Cancelled ->
                        _state.update { it.copy(purchasingProductId = null) }
                }
            }
        }
    }

    private fun verifyToken(purchaseToken: String, productId: String) {
        _state.update { it.copy(purchasingProductId = productId, error = null, message = null) }
        viewModelScope.launch {
            try {
                val response = billingApi.verify(purchaseToken = purchaseToken, productId = productId)
                _state.update {
                    it.copy(
                        purchasingProductId = null,
                        entitlement = response.entitlement,
                        message = if (response.entitlement.plan == "FREE") {
                            "To'lov qabul qilindi, lekin obuna faollashmadi."
                        } else {
                            "Obuna faollashtirildi: ${response.entitlement.plan}."
                        },
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.update {
                    it.copy(purchasingProductId = null, error = error.userMessage())
                }
            }
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { PaywallViewModel(container.billingApi, container.playBilling) }
        }
    }
}
