package com.coach.screentime.tasks

import com.coach.screentime.auth.GoogleAuthRepository
import com.coach.screentime.data.db.dao.TaskDao
import com.coach.screentime.data.db.dao.TaskStateDao
import com.coach.screentime.data.db.entities.TaskEntity
import com.coach.screentime.data.db.entities.TaskStateEntity
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pulls tasks from Google, mirrors them to Room, and seeds task_states for
 * any new ones. Authoritative copy stays on Google's side; the mirror exists
 * so workers can decide what to prompt without an API round-trip every time.
 */
@Singleton
class TasksRepository @Inject constructor(
    private val authRepo: GoogleAuthRepository,
    private val taskDao: TaskDao,
    private val taskStateDao: TaskStateDao,
    private val okHttp: OkHttpClient,
) {
    private val api: GoogleTasksApi by lazy {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        Retrofit.Builder()
            .baseUrl("https://www.googleapis.com/")
            .client(okHttp)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GoogleTasksApi::class.java)
    }

    /** Pulls all open tasks from all lists, mirrors them, seeds states. */
    suspend fun sync(): Result<Int> = withContext(Dispatchers.IO) {
        runCatching {
            val token = authRepo.getAccessToken().getOrThrow()
            val auth = "Bearer $token"
            val lists = api.listTaskLists(auth).items.orEmpty()
            val now = System.currentTimeMillis()
            val mirrored = mutableListOf<TaskEntity>()
            lists.forEach { list ->
                val resp = api.listTasks(list.id, auth, showCompleted = false, showHidden = false)
                val items = resp.items.orEmpty()
                items.forEach { t ->
                    if (t.title.isNullOrBlank()) return@forEach
                    mirrored += TaskEntity(
                        googleId = t.id,
                        listId = list.id,
                        title = t.title,
                        notes = t.notes.orEmpty(),
                        dueDateMs = t.due?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() },
                        completed = t.status == "completed",
                        etag = t.etag.orEmpty(),
                        fetchedAt = now,
                    )
                }
            }
            taskDao.upsertAll(mirrored)
            taskDao.pruneOthers(mirrored.map { it.googleId })

            // Seed states for new tasks; don't disturb existing state for known ones.
            mirrored.forEach { t ->
                if (taskStateDao.byId(t.googleId) == null) {
                    taskStateDao.upsert(
                        TaskStateEntity(
                            googleId = t.googleId,
                            state = STATE_UNTOUCHED,
                            lastPromptedAt = 0L,
                            workingSinceAt = 0L,
                            delayedUntilMs = 0L,
                            lastJudgmentTs = 0L,
                            silenceCheckedAt = 0L,
                        )
                    )
                }
            }
            mirrored.size
        }
    }

    fun signOut() {
        authRepo.signOut()
    }

    suspend fun clearMirror() {
        taskDao.deleteAll()
    }

    companion object {
        const val STATE_UNTOUCHED = "untouched"
        const val STATE_PROMPTED = "prompted"
        const val STATE_WORKING = "working"
        const val STATE_DELAYED = "delayed"
        const val STATE_DISMISSED_PENDING = "dismissed_pending"
        const val STATE_JUDGED_DONE = "judged_done"
    }
}
