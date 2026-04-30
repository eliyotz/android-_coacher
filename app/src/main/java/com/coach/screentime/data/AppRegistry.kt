package com.coach.screentime.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.coach.screentime.data.db.dao.AppDao
import com.coach.screentime.data.db.dao.CategoryDao
import com.coach.screentime.data.db.entities.AppEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/** Maintains the apps table from the device's installed packages. */
@Singleton
class AppRegistry @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDao: AppDao,
    private val categoryDao: CategoryDao,
) {
    suspend fun seedCategoriesIfEmpty() {
        if (categoryDao.snapshot().isEmpty()) {
            categoryDao.upsertAll(Categories.seed)
        }
    }

    /** Scan installed apps and upsert any new ones; preserve user-set fields on existing rows. */
    suspend fun refreshInstalledApps() = withContext(Dispatchers.IO) {
        seedCategoriesIfEmpty()
        val pm = context.packageManager
        val installed = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 || pm.getLaunchIntentForPackage(it.packageName) != null }
        val rows = installed.map { info ->
            val existing = appDao.byPackage(info.packageName)
            val name = pm.getApplicationLabel(info).toString()
            existing?.copy(displayName = name)
                ?: AppEntity(
                    packageName = info.packageName,
                    displayName = name,
                    categoryId = Categories.categoryFor(info.packageName),
                )
        }
        appDao.upsertAll(rows)
    }

    suspend fun ensureAppRow(packageName: String) = withContext(Dispatchers.IO) {
        val existing = appDao.byPackage(packageName)
        if (existing != null) return@withContext
        val pm = context.packageManager
        val name = runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrDefault(packageName)
        appDao.upsert(
            AppEntity(
                packageName = packageName,
                displayName = name,
                categoryId = Categories.categoryFor(packageName),
            )
        )
    }
}
