package com.coach.screentime.ai

import android.util.Log
import com.coach.screentime.data.db.dao.AppDao
import com.coach.screentime.data.db.dao.CategoryDao
import com.coach.screentime.data.db.dao.GoalDao
import com.coach.screentime.data.db.dao.InterventionDao
import com.coach.screentime.data.db.dao.RollupDao
import com.coach.screentime.data.db.entities.AiVerdictEntity
import com.coach.screentime.data.store.SettingsStore
import com.coach.screentime.data.store.Strictness
import com.coach.screentime.util.Time
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Bridges the negotiation overlay to the Gemini judge prompt.
 *
 * Not a real Compose ViewModel (overlays don't run inside the Activity ViewModelStore),
 * but it serves the same role: stateful coordinator for the UI.
 */
class NegotiationViewModel(
    private val geminiClient: GeminiClient,
    private val interventionDao: InterventionDao,
    private val settingsStore: SettingsStore,
    private val goalDao: GoalDao,
    private val rollupDao: RollupDao,
    private val categoryDao: CategoryDao,
    private val appDao: AppDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val verdictAdapter = moshi.adapter(AiVerdict::class.java)

    fun judge(
        packageName: String,
        appLabel: String,
        categoryName: String,
        usedMinutes: Int,
        triggerKind: String,
        reason: String,
        interventionId: Long,
        onResult: (Verdict) -> Unit,
    ) {
        scope.launch {
            val verdict = runJudge(packageName, appLabel, categoryName, usedMinutes, triggerKind, reason, interventionId)
            withContext(Dispatchers.Main) { onResult(verdict) }
        }
    }

    private suspend fun runJudge(
        packageName: String,
        appLabel: String,
        categoryName: String,
        usedMinutes: Int,
        triggerKind: String,
        reason: String,
        interventionId: Long,
    ): Verdict {
        val settings = settingsStore.snapshot()

        // Pre-flight: detect obvious prompt-injection attempts in the user's reason
        // and short-circuit without calling Gemini. Saves quota and prevents the
        // model from being argued into accepting on the basis of override text.
        if (looksLikeInjection(reason)) {
            val verdict = Verdict(
                accept = false,
                extensionMinutes = settings.extensionMinutes,
                explanation = "That looks like an attempt to override the coach. The reason has to be about your actual situation.",
            )
            interventionDao.insertVerdict(
                AiVerdictEntity(
                    interventionId = interventionId,
                    reasonText = reason,
                    verdict = "reject",
                    explanation = verdict.explanation,
                    rawResponse = "[blocked: injection attempt]",
                    ts = System.currentTimeMillis(),
                )
            )
            return verdict
        }

        val goal = goalDao.activeGoal()?.text.orEmpty()
        val priorReasons = priorReasonsToday(packageName)

        val systemPrompt = JudgePrompt.system(settings.strictness, goal)
        val userPrompt = JudgePrompt.user(
            appLabel = appLabel,
            categoryName = categoryName,
            usedMinutes = usedMinutes,
            triggerKind = triggerKind,
            priorReasonsToday = priorReasons,
            timeOfDay = LocalDateTime.now(ZoneId.systemDefault()),
            reason = reason,
        )

        val result = geminiClient.generate(systemPrompt, userPrompt, responseSchema = NEGOTIATION_SCHEMA)
        Log.d("CoachDebug", "raw=${result.getOrNull()} err=${result.exceptionOrNull()?.message}")
        val parsed = result.fold(
            onSuccess = { raw -> parseVerdict(raw) ?: fallback(settings.strictness, settings.extensionMinutes, "Couldn't parse coach response.") },
            onFailure = { fallback(settings.strictness, settings.extensionMinutes, "Service temporarily unavailable.") },
        )

        interventionDao.insertVerdict(
            AiVerdictEntity(
                interventionId = interventionId,
                reasonText = reason,
                verdict = if (parsed.accept) "accept" else "reject",
                explanation = parsed.explanation,
                rawResponse = result.getOrDefault(""),
                ts = System.currentTimeMillis(),
            )
        )
        return parsed
    }

    /**
     * Coarse regex check for the most common prompt-injection patterns. Not a
     * security boundary — the system prompt is the real defense — but this saves
     * a round-trip and avoids the awkwardness of the model occasionally being
     * convinced. False positives are recoverable: the user just rephrases.
     */
    private fun looksLikeInjection(reason: String): Boolean {
        val s = reason.lowercase().trim()
        if (s.length < 4) return false
        return INJECTION_PATTERNS.any { it.containsMatchIn(s) }
    }

    private companion object {
        /** JSON Schema enforced for the negotiation/overlay Gemini response. */
        val NEGOTIATION_SCHEMA = Schema(
            type = "OBJECT",
            properties = mapOf(
                "verdict" to Schema(type = "STRING", enumValues = listOf("accept", "reject")),
                "extensionMinutes" to Schema(type = "INTEGER"),
                "explanation" to Schema(type = "STRING"),
            ),
            required = listOf("verdict", "extensionMinutes", "explanation"),
        )

        val INJECTION_PATTERNS = listOf(
            Regex("\\b(ignore|disregard|forget|override)\\b.*\\b(previous|prior|all|above|earlier)\\b.*\\b(instruction|prompt|rule|directive)\\b"),
            Regex("\\byou (must|have to|should|need to) (say|answer|respond with|reply|output|return) (accept|yes|grant|approve)"),
            Regex("\\bsystem (prompt|instruction|message|role)\\b"),
            Regex("\\b(roleplay|role[- ]play|pretend (you|to))\\b"),
            Regex("\\b(jailbreak|dan mode|developer mode)\\b"),
            Regex("\\boutput[ \"']*\\{[ \"']*verdict[ \"']*[:=][ \"']*accept"),
            Regex("\"verdict\"\\s*:\\s*\"accept\""),
        )
    }

    private fun parseVerdict(raw: String): Verdict? {
        val stripped = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return tryParseJson(stripped) ?: extractFirstJsonObject(stripped)?.let { tryParseJson(it) }
    }

    private fun tryParseJson(json: String): Verdict? = runCatching {
        val v = verdictAdapter.fromJson(json) ?: return null
        Verdict(
            accept = v.verdict.equals("accept", ignoreCase = true),
            extensionMinutes = v.extensionMinutes.coerceIn(5, 30),
            explanation = v.explanation.take(220),
        )
    }.getOrNull()

    private fun extractFirstJsonObject(text: String): String? {
        val start = text.indexOf('{').takeIf { it >= 0 } ?: return null
        var depth = 0
        for (i in start until text.length) {
            when (text[i]) {
                '{' -> depth++
                '}' -> if (--depth == 0) return text.substring(start, i + 1)
            }
        }
        return null
    }

    private fun fallback(strictness: Strictness, defaultMinutes: Int, why: String): Verdict = when (strictness) {
        Strictness.STRICT -> Verdict(false, defaultMinutes, "$why Default to denying.")
        Strictness.BALANCED -> Verdict(false, defaultMinutes, "$why Default to denying — try again later.")
        Strictness.GENTLE -> Verdict(true, defaultMinutes, "$why Granting $defaultMinutes min as a fallback.")
    }

    private suspend fun priorReasonsToday(packageName: String): List<String> {
        val sinceTs = Time.startOfDayMillis(java.time.LocalDate.now())
        return interventionDao.recentVerdictsForApp(packageName, sinceTs, 5).map { it.reasonText }
    }

    data class Verdict(
        val accept: Boolean,
        val extensionMinutes: Int,
        val explanation: String,
    )
}
