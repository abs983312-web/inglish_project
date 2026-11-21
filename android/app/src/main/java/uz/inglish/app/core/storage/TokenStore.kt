package uz.inglish.app.core.storage

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.authDataStore by preferencesDataStore(name = "auth")

/**
 * Persists the access/refresh pair encrypted with a Keystore-backed AES key.
 * Legacy plaintext values from older builds are migrated on first read.
 */
class TokenStore(private val context: Context) {
    private val accessKey = stringPreferencesKey("access_token")
    private val refreshKey = stringPreferencesKey("refresh_token")
    private val cipher = TokenCipher()

    suspend fun save(accessToken: String, refreshToken: String) {
        withContext(Dispatchers.IO) {
            val encryptedAccess = cipher.encrypt(accessToken)
            val encryptedRefresh = cipher.encrypt(refreshToken)
            context.authDataStore.edit { prefs ->
                prefs[accessKey] = encryptedAccess
                prefs[refreshKey] = encryptedRefresh
            }
        }
    }

    suspend fun accessToken(): String? = read(accessKey)

    suspend fun refreshToken(): String? = read(refreshKey)

    suspend fun clear() {
        withContext(Dispatchers.IO) {
            context.authDataStore.edit { prefs -> prefs.clear() }
            // Drop the key too, so a logged-out session leaves no usable
            // ciphertext behind.
            runCatching { cipher.deleteKey() }
        }
    }

    private suspend fun read(key: Preferences.Key<String>): String? =
        withContext(Dispatchers.IO) {
            val stored = context.authDataStore.data.map { prefs -> prefs[key] }.first()
                ?: return@withContext null

            val plaintext = try {
                cipher.decrypt(stored)
            } catch (_: Exception) {
                // The Keystore key was invalidated or the value is corrupt.
                // Force a fresh login instead of crashing the app.
                clear()
                return@withContext null
            }

            if (!cipher.isEncrypted(stored)) {
                // Legacy plaintext token: transparently migrate it.
                runCatching {
                    context.authDataStore.edit { prefs ->
                        prefs[key] = cipher.encrypt(plaintext)
                    }
                }
            }

            plaintext
        }
}
