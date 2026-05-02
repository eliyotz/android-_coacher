package com.coach.screentime.tasks

import com.squareup.moshi.JsonClass
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path
import retrofit2.http.Query

interface GoogleTasksApi {
    @GET("tasks/v1/users/@me/lists")
    suspend fun listTaskLists(
        @Header("Authorization") authorization: String,
        @Query("maxResults") maxResults: Int = 50,
    ): TaskListsResponse

    @GET("tasks/v1/lists/{taskList}/tasks")
    suspend fun listTasks(
        @Path("taskList") taskListId: String,
        @Header("Authorization") authorization: String,
        @Query("showCompleted") showCompleted: Boolean = false,
        @Query("showHidden") showHidden: Boolean = false,
        @Query("maxResults") maxResults: Int = 100,
    ): TasksResponse
}

@JsonClass(generateAdapter = true)
data class TaskListsResponse(val items: List<TaskListResource>?)

@JsonClass(generateAdapter = true)
data class TaskListResource(val id: String, val title: String?)

@JsonClass(generateAdapter = true)
data class TasksResponse(val items: List<TaskResource>?)

@JsonClass(generateAdapter = true)
data class TaskResource(
    val id: String,
    val etag: String? = null,
    val title: String? = null,
    val notes: String? = null,
    val due: String? = null,            // RFC 3339 e.g. "2026-05-04T00:00:00.000Z"
    val status: String? = null,         // "needsAction" | "completed"
    val completed: String? = null,      // RFC 3339 timestamp if completed
)
