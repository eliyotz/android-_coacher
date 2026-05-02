package com.coach.screentime.auth

import com.squareup.moshi.JsonClass
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Google OAuth 2.0 token endpoint.
 * - exchange auth code → refresh + access tokens (one-time, during onboarding)
 * - refresh access token via refresh token (every ~1 hour)
 *
 * For installed app flow, redirect URI is "" (offline access via SERVER_AUTH_CODE).
 */
interface OAuthApi {
    @FormUrlEncoded
    @POST("token")
    suspend fun exchangeCode(
        @Field("code") code: String,
        @Field("client_id") clientId: String,
        @Field("redirect_uri") redirectUri: String,
        @Field("grant_type") grantType: String = "authorization_code",
    ): TokenResponse

    @FormUrlEncoded
    @POST("token")
    suspend fun refresh(
        @Field("refresh_token") refreshToken: String,
        @Field("client_id") clientId: String,
        @Field("grant_type") grantType: String = "refresh_token",
    ): TokenResponse
}

@JsonClass(generateAdapter = true)
data class TokenResponse(
    val access_token: String? = null,
    val refresh_token: String? = null,
    val expires_in: Long? = null,
    val token_type: String? = null,
    val scope: String? = null,
)

object OAuthClient {
    fun create(): OAuthApi {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        return Retrofit.Builder()
            .baseUrl("https://oauth2.googleapis.com/")
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(OAuthApi::class.java)
    }
}
