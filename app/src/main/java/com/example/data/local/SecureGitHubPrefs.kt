package com.example.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class GitHubBuildConfig(
    val token: String = "",
    val owner: String = "",
    val repo: String = "",
    val branch: String = "main",
    val workflowFile: String = "build-apk.yml"
) {
    val isConfigured: Boolean
        get() = token.isNotBlank() && owner.isNotBlank() && repo.isNotBlank()
}

class SecureGitHubPrefs(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadConfig(): GitHubBuildConfig {
        val encryptedToken = prefs.getString(KEY_TOKEN_ENC, null)
        val iv = prefs.getString(KEY_TOKEN_IV, null)
        val decryptedToken = if (!encryptedToken.isNullOrBlank() && !iv.isNullOrBlank()) {
            decryptToken(encryptedToken, iv)
        } else {
            ""
        }

        return GitHubBuildConfig(
            token = decryptedToken,
            owner = prefs.getString(KEY_OWNER, "").orEmpty(),
            repo = prefs.getString(KEY_REPO, "").orEmpty(),
            branch = prefs.getString(KEY_BRANCH, "main").orEmpty().ifBlank { "main" },
            workflowFile = prefs.getString(KEY_WORKFLOW, "build-apk.yml").orEmpty().ifBlank { "build-apk.yml" }
        )
    }

    /** Returns false if the token could not be encrypted; in that case no token is stored. */
    fun saveConfig(config: GitHubBuildConfig): Boolean {
        val editor = prefs.edit()
            .putString(KEY_OWNER, config.owner.trim())
            .putString(KEY_REPO, config.repo.trim())
            .putString(KEY_BRANCH, config.branch.trim().ifBlank { "main" })
            .putString(KEY_WORKFLOW, config.workflowFile.trim().ifBlank { "build-apk.yml" })

        var tokenStored = true
        if (config.token.isBlank()) {
            editor.remove(KEY_TOKEN_ENC).remove(KEY_TOKEN_IV)
        } else {
            val encrypted = encryptToken(config.token.trim())
            if (encrypted == null) {
                // Never fall back to plaintext storage.
                editor.remove(KEY_TOKEN_ENC).remove(KEY_TOKEN_IV)
                tokenStored = false
            } else {
                editor.putString(KEY_TOKEN_ENC, encrypted.first).putString(KEY_TOKEN_IV, encrypted.second)
            }
        }
        editor.apply()
        return tokenStored
    }

    /** Returns (apiKey, model). */
    fun loadGemini(): Pair<String, String> {
        val enc = prefs.getString(KEY_GEMINI_ENC, null)
        val iv = prefs.getString(KEY_GEMINI_IV, null)
        val key = if (!enc.isNullOrBlank() && !iv.isNullOrBlank()) decryptToken(enc, iv) else ""
        return key to prefs.getString(KEY_GEMINI_MODEL, "").orEmpty()
    }

    /** Returns false if the key could not be encrypted (nothing is stored in that case). */
    fun saveGemini(apiKey: String, model: String): Boolean {
        val editor = prefs.edit().putString(KEY_GEMINI_MODEL, model.trim())
        var ok = true
        if (apiKey.isBlank()) {
            editor.remove(KEY_GEMINI_ENC).remove(KEY_GEMINI_IV)
        } else {
            val enc = encryptToken(apiKey.trim())
            if (enc == null) {
                editor.remove(KEY_GEMINI_ENC).remove(KEY_GEMINI_IV)
                ok = false
            } else {
                editor.putString(KEY_GEMINI_ENC, enc.first).putString(KEY_GEMINI_IV, enc.second)
            }
        }
        editor.apply()
        return ok
    }

    fun clearSecrets() {
        prefs.edit().clear().apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        generator.init(spec)
        return generator.generateKey()
    }

    private fun encryptToken(plainText: String): Pair<String, String>? {
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
            val encB64 = Base64.encodeToString(encrypted, Base64.NO_WRAP)
            val ivB64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
            encB64 to ivB64
        } catch (_: Exception) {
            null
        }
    }

    private fun decryptToken(encB64: String, ivB64: String): String {
        return try {
            if (ivB64.isBlank()) {
                return String(Base64.decode(encB64, Base64.NO_WRAP), Charsets.UTF_8)
            }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val iv = Base64.decode(ivB64, Base64.NO_WRAP)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            val decoded = Base64.decode(encB64, Base64.NO_WRAP)
            String(cipher.doFinal(decoded), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    companion object {
        private const val PREFS_NAME = "forge_ide_github_secrets"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "forge_ide_gh_pat_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"

        private const val KEY_TOKEN_ENC = "gh_token_enc"
        private const val KEY_TOKEN_IV = "gh_token_iv"
        private const val KEY_OWNER = "gh_owner"
        private const val KEY_REPO = "gh_repo"
        private const val KEY_BRANCH = "gh_branch"
        private const val KEY_WORKFLOW = "gh_workflow"
        private const val KEY_GEMINI_ENC = "gm_key_enc"
        private const val KEY_GEMINI_IV = "gm_key_iv"
        private const val KEY_GEMINI_MODEL = "gm_model"
    }
}
