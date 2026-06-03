package com.coach.screentime.di

import com.coach.screentime.ai.GeminiApi
import com.coach.screentime.ai.GeminiClient
import com.coach.screentime.ai.GeminiRestClient
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        // Privacy/security: never log request/response bodies. The Gemini request URL carries
        // the API key as a query param and the body carries the user's personal prompt — BODY
        // logging would leak both to logcat. Keep this at NONE.
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.NONE))
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttp: OkHttpClient, moshi: Moshi): Retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun provideGeminiApi(retrofit: Retrofit): GeminiApi = retrofit.create(GeminiApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class GeminiBindModule {
    @Binds
    abstract fun bindGeminiClient(impl: GeminiRestClient): GeminiClient
}
