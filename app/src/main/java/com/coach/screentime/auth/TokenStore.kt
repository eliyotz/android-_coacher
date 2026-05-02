package com.coach.screentime.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encrypted at-rest storage for OAuth refresh + access tokens.
 * Refresh tokens never expire (until revoked); we only call the auth flow
 * once during onboarding and then refresh access tokens forever.
 */
@Singleton
class TokenStore @Inject constructor(
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

    val refreshToken: String? get() = prefs.getString(KEY_REFRESH, null)?.takeIf { it.isNotBlank() }
    val accessToken: String? get() = prefs.getString(KEY_ACCESS, null)?.takeIf { it.isNotBlank() }
    val accessExpiresAt: Long get() = prefs.getLong(KEY_ACCESS_EXP, 0L)
    val accountEmail: String? get() = prefs.getString(KEY_EMAIL, null)?.takeIf { it.isNotBlank() }

    fun saveRefresh(refresh: String, email: String?) = prefs.edit()
        .putString(KEY_REFRESH, refresh)
        .also { if (email != null) it.putString(KEY_EMAIL, email) }
        .apply()

    fun saveAccess(access: String, expiresAt: Long) = prefs.edit()
        .putString(KEY_ACCESS, access)
        .putLong(KEY_ACCESS_EXP, expiresAt)
        .apply()

    fun clear() = prefs.edit().clear().apply()

    fun isConnected(): Boolean = !refreshToken.isNullOrBlank()

    private companion object {
        const val FILE = "coach_oauth"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_ACCESS = "access_token"
        const val KEY_ACCESS_EXP = "access_expires_at"
        const val KEY_EMAIL = "account_email"
    }
}
