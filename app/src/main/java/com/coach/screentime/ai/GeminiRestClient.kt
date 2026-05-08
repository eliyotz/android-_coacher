package com.coach.screentime.ai

import android.util.Log
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
                        maxOutputTokens = 1024,
                        responseMimeType = "application/json",
                        thinkingConfig = ThinkingConfig(thinkingBudget = 0),
                        responseSchema = Schema(
                            type = "OBJECT",
                            properties = mapOf(
                                "verdict" to Schema(type = "STRING", enumValues = listOf("accept", "reject")),
                                "extensionMinutes" to Schema(type = "INTEGER"),
                                "explanation" to Schema(type = "STRING"),
                            ),
                            required = listOf("verdict", "extensionMinutes", "explanation"),
                        ),
                    ),
                ),
            )
            val candidate = resp.candidates?.firstOrNull()
            val parts = candidate?.content?.parts
            Log.d("CoachDebug", "candidates=${resp.candidates?.size} parts=${parts?.size}")
            parts?.forEachIndexed { i, p -> Log.d("CoachDebug", "part[$i] text=${p.text}") }
            parts?.joinToString("") { it.text.orEmpty() }
                ?: throw IllegalStateException("Empty response from Gemini")
        }
    }

    companion object {
        // "flash-latest" tracks whatever the current Flash family is — fast, cheap, generous free tier.
        private const val MODEL = "gemini-flash-latest"
    }
}
