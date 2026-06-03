package com.coach.screentime.ai

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.coach.screentime.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runtime, encrypted-at-rest storage for the Gemini API key, so the secret does
 * NOT have to be compiled into the APK as a BuildConfig constant.
 *
 * Resolution order for [geminiKey]:
 *   1. a key the user saved in Settings (encrypted on-device via Keystore), else
 *   2. the build-time [BuildConfig.GEMINI_API_KEY] fallback (may be blank).
 *
 * Leaving GEMINI_API_KEY blank in local.properties and entering the key in
 * Settings keeps the secret out of the shipped binary entirely. Mirrors the
 * EncryptedSharedPreferences setup already used by [com.coach.screentime.auth.TokenStore].
 */
@Singleton
class ApiKeyStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences = run {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    /** True when the user has saved a runtime key (independent of the build fallback). */
    fun hasUserKey(): Boolean = !storedKey().isNullOrBlank()

    /** True when a usable key is available from either source. */
    fun hasGeminiKey(): Boolean = geminiKey().isNotBlank()

    /** The user-provided runtime key if set, else the build-time fallback (possibly blank). */
    fun geminiKey(): String = storedKey()?.takeIf { it.isNotBlank() } ?: BuildConfig.GEMINI_API_KEY

    fun setGeminiKey(value: String) = prefs.edit().putString(KEY_GEMINI, value.trim()).apply()

    fun clearGeminiKey() = prefs.edit().remove(KEY_GEMINI).apply()

    private fun storedKey(): String? = prefs.getString(KEY_GEMINI, null)

    private companion object {
        const val FILE = "coach_secure_keys"
        const val KEY_GEMINI = "gemini_api_key"
    }
}
