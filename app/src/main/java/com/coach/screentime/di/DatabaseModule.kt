package com.coach.screentime.di

import android.content.Context
import androidx.room.Room
import com.coach.screentime.data.db.AppDatabase
import com.coach.screentime.data.db.dao.AppDao
import com.coach.screentime.data.db.dao.CategoryDao
import com.coach.screentime.data.db.dao.GoalDao
import com.coach.screentime.data.db.dao.InterventionDao
import com.coach.screentime.data.db.dao.ReportDao
import com.coach.screentime.data.db.dao.RollupDao
import com.coach.screentime.data.db.dao.SessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "coach.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun appDao(db: AppDatabase): AppDao = db.appDao()
    @Provides fun categoryDao(db: AppDatabase): CategoryDao = db.categoryDao()
    @Provides fun sessionDao(db: AppDatabase): SessionDao = db.sessionDao()
    @Provides fun rollupDao(db: AppDatabase): RollupDao = db.rollupDao()
    @Provides fun interventionDao(db: AppDatabase): InterventionDao = db.interventionDao()
    @Provides fun reportDao(db: AppDatabase): ReportDao = db.reportDao()
    @Provides fun goalDao(db: AppDatabase): GoalDao = db.goalDao()
}
