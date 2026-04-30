package com.coach.screentime.ai

import com.coach.screentime.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRestClient @Inject constructor(
    private val api: GeminiApi,
) : GeminiClient {

    override suspend fun generate(systemPrompt: String, userPrompt: String): Result<String> {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank()) {
            return Result.failure(IllegalStateException("Gemini API key not configured"))
        }
        return runCatching {
            val resp = api.generate(
                model = MODEL,
                apiKey = key,
                body = GenerateRequest(
                    systemInstruction = Content(parts = listOf(Part(systemPrompt))),
                    contents = listOf(Content(role = "user", parts = listOf(Part(userPrompt)))),
                    generationConfig = GenerationConfig(
                        temperature = 0.2,
                        maxOutputTokens = 512,
                        responseMimeType = "application/json",
                    ),
                ),
            )
            resp.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.joinToString("") { it.text }
                ?: throw IllegalStateException("Empty response from Gemini")
        }
    }

    companion object {
        // "flash-latest" tracks whatever the current Flash family is — fast, cheap, generous free tier.
        private const val MODEL = "gemini-flash-latest"
    }
}
