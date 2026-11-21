package uz.inglish.app.core.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * AES-256-GCM envelope backed by the Android Keystore.
 *
 * Stored format: `v1:<base64 iv>:<base64 ciphertext>`. Values without the
 * prefix are legacy plaintext tokens written before encryption existed; they
 * are accepted on read and re-encrypted on the next write.
 *
 * The key never leaves the Keystore and is not exported to app storage.
 */
internal class TokenCipher(
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
) {
    @Synchronized
    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return PREFIX +
            Base64.encodeToString(iv, Base64.NO_WRAP) +
            SEPARATOR +
            Base64.encodeToString(ciphertext, Base64.NO_WRAP)
    }

    @Synchronized
    fun decrypt(stored: String): String {
        if (!isEncrypted(stored)) return stored

        val parts = stored.removePrefix(PREFIX).split(SEPARATOR)
        require(parts.size == 2) { "Malformed encrypted token" }

        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val ciphertext = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    fun isEncrypted(stored: String): Boolean = stored.startsWith(PREFIX)

    @Synchronized
    fun deleteKey() {
        val keyStore = loadKeyStore()
        if (keyStore.containsAlias(keyAlias)) {
            keyStore.deleteEntry(keyAlias)
        }
    }

    private fun secretKey(): SecretKey {
        val keyStore = loadKeyStore()
        (keyStore.getEntry(keyAlias, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private fun loadKeyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_BITS = 128
        private const val PREFIX = "v1:"
        private const val SEPARATOR = ":"
        private const val DEFAULT_KEY_ALIAS = "inglish_token_key_v1"
    }
}
