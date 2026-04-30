package com.coach.screentime.ai

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

        val result = geminiClient.generate(systemPrompt, userPrompt)
        val parsed = result.fold(
            onSuccess = { raw -> parseVerdict(raw) ?: fallback(settings.strictness, settings.extensionMinutes, "Couldn't parse coach response.") },
            onFailure = { fallback(settings.strictness, settings.extensionMinutes, "Coach unreachable: ${it.message ?: "unknown error"}") },
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

    private fun parseVerdict(raw: String): Verdict? {
        val trimmed = raw.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return runCatching {
            val v = verdictAdapter.fromJson(trimmed) ?: return null
            val accept = v.verdict.equals("accept", ignoreCase = true)
            Verdict(
                accept = accept,
                extensionMinutes = v.extensionMinutes.coerceIn(5, 30),
                explanation = v.explanation.take(220),
            )
        }.getOrNull()
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
