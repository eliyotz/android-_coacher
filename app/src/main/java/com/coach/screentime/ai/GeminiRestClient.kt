package com.coach.screentime.ai

import android.util.Log
import com.coach.screentime.BuildConfig
import kotlinx.coroutines.delay
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GeminiRestClient @Inject constructor(
    private val api: GeminiApi,
) : GeminiClient {

    override suspend fun generate(
        systemPrompt: String,
        userPrompt: String,
        responseSchema: Schema?,
    ): Result<String> {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isBlank()) {
            return Result.failure(IllegalStateException("Gemini API key not configured"))
        }

        var lastError: Throwable? = null
        var delayMs = 2_000L
        repeat(MAX_RETRIES) { attempt ->
            if (attempt > 0) {
                Log.d("CoachDebug", "Gemini retry #$attempt after ${delayMs}ms (prev: ${lastError?.message})")
                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(30_000L)
            }

            val result = runCatching {
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
                            responseSchema = responseSchema,
                            // thinkingBudget=0: disable thinking so all tokens go to output.
                            // Required for gemini-2.5-flash which is a thinking model.
                            thinkingConfig = ThinkingConfig(thinkingBudget = 0),
                        ),
                    ),
                )
                val candidate = resp.candidates?.firstOrNull()
                val parts = candidate?.content?.parts
                Log.d("CoachDebug", "Gemini ok: candidates=${resp.candidates?.size} parts=${parts?.size}")
                parts?.joinToString("") { it.text.orEmpty() }
                    ?: throw IllegalStateException("Empty response from Gemini")
            }

            if (result.isSuccess) return result

            val ex = result.exceptionOrNull()!!
            Log.e("CoachDebug", "Gemini attempt $attempt failed: ${ex.message}")

            // Retry on transient server-side errors only
            val isRetryable = ex is HttpException && ex.code() in listOf(429, 500, 503)
            if (!isRetryable) return Result.failure(ex)

            lastError = ex
        }
        return Result.failure(lastError ?: IllegalStateException("All Gemini retries exhausted"))
    }

    companion object {
        // gemini-2.5-flash: thinking model with thinkingBudget=0 for fast, cheap structured output.
        // Has a separate quota bucket from 2.0-flash on the free tier.
        private const val MODEL = "gemini-2.5-flash"
        private const val MAX_RETRIES = 3
    }
}
