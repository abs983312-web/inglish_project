package uz.inglish.app.core

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import uz.inglish.app.core.billing.PlayBillingManager
import uz.inglish.app.core.network.ApiClient
import uz.inglish.app.core.network.AuthApi
import uz.inglish.app.core.network.BattleApi
import uz.inglish.app.core.network.BattlePackApi
import uz.inglish.app.core.network.BattleRealtime
import uz.inglish.app.core.network.BillingApi
import uz.inglish.app.core.network.ContentApi
import uz.inglish.app.core.network.PlacementApi
import uz.inglish.app.core.network.ProfileApi
import uz.inglish.app.core.network.ProgressApi
import uz.inglish.app.core.network.SpeakingApi
import uz.inglish.app.core.network.VocabularyApi
import uz.inglish.app.core.network.WritingApi
import uz.inglish.app.core.storage.TokenStore

class AppContainer(context: Context) {
    val tokenStore = TokenStore(context.applicationContext)

    private val httpClient = HttpClient(OkHttp) {
        expectSuccess = false

        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    // Optional request fields (e.g. speaking duration) must be
                    // omitted instead of serialised as `null`; the backend
                    // schemas use `.optional()`, not `.nullable()`.
                    explicitNulls = false
                },
            )
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
            connectTimeoutMillis = 15_000
            socketTimeoutMillis = 30_000
        }
    }

    val apiClient = ApiClient(httpClient, tokenStore)

    // WebSocket sessions must not inherit the 30s HTTP request timeout.
    private val webSocketClient = HttpClient(OkHttp) {
        expectSuccess = false
        install(WebSockets)
    }

    val authApi = AuthApi(apiClient)
    val profileApi = ProfileApi(apiClient)
    val billingApi = BillingApi(apiClient)
    val progressApi = ProgressApi(apiClient)
    val contentApi = ContentApi(apiClient)
    val writingApi = WritingApi(apiClient)
    val speakingApi = SpeakingApi(apiClient)
    val vocabularyApi = VocabularyApi(apiClient)
    val battleApi = BattleApi(apiClient)
    val battlePackApi = BattlePackApi(apiClient)
    val battleRealtime = BattleRealtime(webSocketClient, apiClient)
    val placementApi = PlacementApi(apiClient)
    val playBilling = PlayBillingManager(context.applicationContext)
}
