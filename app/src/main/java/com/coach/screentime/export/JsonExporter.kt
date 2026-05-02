package com.coach.screentime.export

import android.content.Context
import android.net.Uri
import com.coach.screentime.data.db.dao.AppDao
import com.coach.screentime.data.db.dao.CategoryDao
import com.coach.screentime.data.db.dao.GoalDao
import com.coach.screentime.data.db.dao.InterventionDao
import com.coach.screentime.data.db.dao.NudgeDao
import com.coach.screentime.data.db.dao.ReflectionDao
import com.coach.screentime.data.db.dao.ReportDao
import com.coach.screentime.data.db.dao.RollupDao
import com.coach.screentime.data.db.dao.SessionDao
import com.coach.screentime.util.Time
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Writes a JSON dump of the user's data to a content URI (typically obtained
 * via the system Storage Access Framework — `ACTION_CREATE_DOCUMENT`).
 *
 * One snapshot at a time; not streaming. The dataset is small enough that
 * keeping it in memory is fine even after a year of dogfooding.
 */
@Singleton
class JsonExporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val sessionDao: SessionDao,
    private val rollupDao: RollupDao,
    private val appDao: AppDao,
    private val categoryDao: CategoryDao,
    private val interventionDao: InterventionDao,
    private val reportDao: ReportDao,
    private val goalDao: GoalDao,
    private val reflectionDao: ReflectionDao,
    private val nudgeDao: NudgeDao,
) {
    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    suspend fun exportTo(uri: Uri): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            // 90-day window for sessions (everything else we just dump in full).
            val since = Time.lastNDaysStrings(90).last()
            val sessions = sessionDao.forDates(Time.lastNDaysStrings(90))
            val rollups = rollupDao.snapshotForDates(Time.lastNDaysStrings(90))
            val apps = appDao.snapshot()
            val categories = categoryDao.snapshot()
            val interventions = interventionDao.since(0L)
            val verdicts = interventionDao.verdictsFor(interventions.map { it.id })
            val reports = reportDao.snapshot()
            val goals = goalDao.activeGoal()?.let { listOf(it) } ?: emptyList<Any>()
            val reflections = reflectionDao.forDates(Time.lastNDaysStrings(365))
            val nudges = nudgeDao.since(0L)

            val payload = mapOf(
                "version" to 1,
                "exportedAt" to System.currentTimeMillis(),
                "windowStartDate" to since,
                "apps" to apps,
                "categories" to categories,
                "sessions" to sessions,
                "dailyRollups" to rollups,
                "interventions" to interventions,
                "aiVerdicts" to verdicts,
                "weeklyReports" to reports,
                "goals" to goals,
                "reflections" to reflections,
                "nudges" to nudges,
            )

            val type = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
            val adapter = moshi.adapter<Map<String, Any?>>(type).indent("  ")
            val json = adapter.toJson(payload)

            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
                out.flush()
            } ?: error("Could not open output stream for $uri")

            json.length.toLong()
        }
    }
}
