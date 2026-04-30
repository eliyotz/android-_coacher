package com.coach.screentime.ai

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AiVerdict(
    val verdict: String,
    val extensionMinutes: Int = 15,
    val explanation: String,
)
