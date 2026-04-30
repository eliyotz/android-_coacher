package com.coach.screentime.ai

interface GeminiClient {
    /** Sends a single-turn prompt and returns the raw text response. */
    suspend fun generate(systemPrompt: String, userPrompt: String): Result<String>
}
