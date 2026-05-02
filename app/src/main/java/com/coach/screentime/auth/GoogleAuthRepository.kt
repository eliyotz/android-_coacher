package com.coach.screentime.auth

import com.coach.screentime.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the Google OAuth state. Two paths:
 *  - [completeSignIn] called by the SettingsActivity after Google's
 *    authorization sheet returns a server auth code. Exchanges it for a
 *    refresh + access token, persists both.
 *  - [getAccessToken] called by anyone needing to call the Tasks API.
 *    Returns the cached access token if it has > 60 s remaining, otherwise
 *    refreshes it via the refresh token. Throws if not signed in.
 */
@Singleton
class GoogleAuthRepository @Inject constructor(
    private val tokenStore: TokenStore,
) {
    private val oauthApi by lazy { OAuthClient.create() }

    val isConnected: Boolean get() = tokenStore.isConnected()
    val accountEmail: String? get() = tokenStore.accountEmail

    fun isOAuthConfigured(): Boolean = BuildConfig.GOOGLE_OAUTH_CLIENT_ID.isNotBlank()

    suspend fun completeSignIn(serverAuthCode: String, email: String?): Result<Unit> {
        if (!isOAuthConfigured()) return Result.failure(IllegalStateException("OAuth client ID not configured"))
        return runCatching {
            val resp = oauthApi.exchangeCode(
                code = serverAuthCode,
                clientId = BuildConfig.GOOGLE_OAUTH_CLIENT_ID,
                // Empty redirect URI is correct for installed apps using PlayServicesAuth's
                // requestServerAuthCode flow with a Web client ID.
                redirectUri = "",
            )
            val refresh = resp.refresh_token ?: error("Google did not return a refresh_token. " +
                "Make sure you requested offline access and forced consent.")
            val access = resp.access_token ?: error("Google did not return an access_token.")
            val exp = System.currentTimeMillis() + ((resp.expires_in ?: 3600L) * 1000L) - 60_000L
            tokenStore.saveRefresh(refresh, email)
            tokenStore.saveAccess(access, exp)
        }
    }

    suspend fun getAccessToken(): Result<String> {
        if (!isOAuthConfigured()) return Result.failure(IllegalStateException("OAuth client ID not configured"))
        val refresh = tokenStore.refreshToken
            ?: return Result.failure(IllegalStateException("Not connected to Google."))
        val cached = tokenStore.accessToken
        val exp = tokenStore.accessExpiresAt
        if (!cached.isNullOrBlank() && System.currentTimeMillis() < exp) {
            return Result.success(cached)
        }
        return runCatching {
            val resp = oauthApi.refresh(
                refreshToken = refresh,
                clientId = BuildConfig.GOOGLE_OAUTH_CLIENT_ID,
            )
            val access = resp.access_token ?: error("Refresh did not return an access_token.")
            val newExp = System.currentTimeMillis() + ((resp.expires_in ?: 3600L) * 1000L) - 60_000L
            tokenStore.saveAccess(access, newExp)
            access
        }
    }

    fun signOut() {
        tokenStore.clear()
    }
}
