package com.coach.screentime.punishment

import com.coach.screentime.data.db.dao.PunishmentDao
import com.coach.screentime.data.db.entities.PunishmentEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI-initiated, targeted punishment (vs Focus mode which is user-initiated and blanket).
 *
 * A punishment may:
 *  - block specific package names for its duration,
 *  - reduce today's per-app + category caps by a percentage,
 *  - force Focus mode for a number of minutes (composes with FocusManager),
 *  - lengthen the mindfulness pause for the rest of the day.
 *
 * The InterventionEngine consults [isAppBlocked], [capReductionPct], and
 * [mindfulPauseMultiplier] on every flagged-app open.
 */
@Singleton
class PunishmentManager @Inject constructor(
    private val punishmentDao: PunishmentDao,
) {
    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val packagesType = Types.newParameterizedType(List::class.java, String::class.java)
    private val packagesAdapter = moshi.adapter<List<String>>(packagesType)

    suspend fun activeNow(now: Long = System.currentTimeMillis()): List<PunishmentEntity> =
        punishmentDao.activeSnapshot(now)

    suspend fun isAppBlocked(packageName: String, now: Long = System.currentTimeMillis()): ActiveBlock? {
        val active = punishmentDao.activeSnapshot(now)
        for (p in active) {
            val packages = runCatching { packagesAdapter.fromJson(p.blockedPackagesJson).orEmpty() }
                .getOrDefault(emptyList())
            if (packageName in packages) return ActiveBlock(p, packages)
        }
        return null
    }

    /** Worst (largest) cap reduction across active punishments, 0..100. */
    suspend fun capReductionPct(now: Long = System.currentTimeMillis()): Int =
        punishmentDao.activeSnapshot(now).maxOfOrNull { it.capReductionPct } ?: 0

    /** Largest multiplier across active punishments, with 1.0 floor. */
    suspend fun mindfulPauseMultiplier(now: Long = System.currentTimeMillis()): Float =
        punishmentDao.activeSnapshot(now)
            .maxOfOrNull { it.mindfulPauseMultiplier }
            ?.coerceAtLeast(1f) ?: 1f

    /** Forced focus minutes still owed across active punishments. */
    suspend fun maxFocusMinutesRemaining(now: Long = System.currentTimeMillis()): Int =
        punishmentDao.activeSnapshot(now).maxOfOrNull {
            // Focus minutes are bounded by punishment expiry.
            val msLeft = it.expiresAt - now
            minOf(it.focusMinutes, (msLeft / 60_000L).toInt().coerceAtLeast(0))
        } ?: 0

    suspend fun apply(
        source: String,
        taskGoogleId: String?,
        blockedPackages: List<String>,
        capReductionPct: Int,
        focusMinutes: Int,
        mindfulPauseMultiplier: Float,
        durationMs: Long,
        rationale: String,
        severity: String,
    ): Long {
        val now = System.currentTimeMillis()
        return punishmentDao.insert(
            PunishmentEntity(
                decidedAt = now,
                source = source,
                taskGoogleId = taskGoogleId,
                expiresAt = now + durationMs.coerceAtLeast(0L),
                blockedPackagesJson = packagesAdapter.toJson(blockedPackages),
                capReductionPct = capReductionPct.coerceIn(0, 90),
                focusMinutes = focusMinutes.coerceAtLeast(0),
                mindfulPauseMultiplier = mindfulPauseMultiplier.coerceIn(1f, 5f),
                rationale = rationale.take(400),
                severity = severity,
            )
        )
    }

    fun parsePackages(json: String): List<String> =
        runCatching { packagesAdapter.fromJson(json).orEmpty() }.getOrDefault(emptyList())

    data class ActiveBlock(val punishment: PunishmentEntity, val blockedPackages: List<String>)
}
