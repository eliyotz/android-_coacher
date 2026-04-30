package com.coach.screentime.ai

import com.squareup.moshi.JsonClass
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generate(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body body: GenerateRequest,
    ): GenerateResponse
}

@JsonClass(generateAdapter = true)
data class GenerateRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null,
    val generationConfig: GenerationConfig? = null,
)

@JsonClass(generateAdapter = true)
data class Content(val role: String? = null, val parts: List<Part>)

@JsonClass(generateAdapter = true)
data class Part(val text: String)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    val temperature: Double? = null,
    val maxOutputTokens: Int? = null,
    val responseMimeType: String? = null,
)

@JsonClass(generateAdapter = true)
data class GenerateResponse(val candidates: List<Candidate>?)

@JsonClass(generateAdapter = true)
data class Candidate(val content: Content?)
