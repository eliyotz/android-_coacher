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

        // On the free tier each model has its own daily quota bucket. When the primary
        // (higher-quality) model exhausts its quota we fall back to the lite sibling — it
        // returns 429 from a separate counter, so this effectively doubles our daily budget.
        var lastError: Throwable? = null
        var delayMs = 2_000L
        repeat(MAX_RETRIES) { attempt ->
            if (attempt > 0) {
                Log.d("CoachDebug", "Gemini retry round #$attempt after ${delayMs}ms (prev: ${lastError?.message})")
                delay(delayMs)
                delayMs = (delayMs * 2).coerceAtMost(30_000L)
            }
            for (model in MODELS) {
                val result = callOnce(model, key, systemPrompt, userPrompt, responseSchema)
                if (result.isSuccess) return result
                val ex = result.exceptionOrNull()!!
                Log.e("CoachDebug", "Gemini[$model] attempt $attempt failed: ${ex.message}")
                val isRetryable = ex is HttpException && ex.code() in listOf(429, 500, 503)
                if (!isRetryable) return Result.failure(ex)
                lastError = ex
                // On 429 we don't backoff before trying the next model — separate quotas.
            }
        }
        return Result.failure(lastError ?: IllegalStateException("All Gemini retries exhausted"))
    }

    private suspend fun callOnce(
        model: String,
        key: String,
        systemPrompt: String,
        userPrompt: String,
        responseSchema: Schema?,
    ): Result<String> = runCatching {
        val resp = api.generate(
            model = model,
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
                    // Required for the 2.5 family (thinking models).
                    thinkingConfig = ThinkingConfig(thinkingBudget = 0),
                ),
            ),
        )
        val candidate = resp.candidates?.firstOrNull()
        val parts = candidate?.content?.parts
        Log.d("CoachDebug", "Gemini[$model] ok: candidates=${resp.candidates?.size} parts=${parts?.size}")
        parts?.joinToString("") { it.text.orEmpty() }
            ?: throw IllegalStateException("Empty response from Gemini")
    }

    companion object {
        // Tried in order; each has a separate free-tier daily quota.
        // 2.5-flash: ~20 req/day, higher quality. lite: ~250 req/day, lower latency.
        private val MODELS = listOf("gemini-2.5-flash", "gemini-2.5-flash-lite")
        private const val MAX_RETRIES = 3
    }
}
