package com.coach.screentime.ai

interface GeminiClient {
    /**
     * Sends a single-turn prompt and returns the raw text response.
     *
     * @param responseSchema Optional Gemini JSON Schema to enforce the response shape.
     *   Pass null (default) to get free-form JSON (model still told to return JSON via
     *   responseMimeType). Each caller is responsible for parsing its own response format.
     */
    suspend fun generate(
        systemPrompt: String,
        userPrompt: String,
        responseSchema: Schema? = null,
    ): Result<String>
}
