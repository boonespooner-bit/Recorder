package com.recorder.app.di

import android.content.Context
import com.google.gson.Gson
import com.recorder.app.data.db.RecordingDao
import com.recorder.app.data.db.RecordingDatabase
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
    fun provideDatabase(@ApplicationContext ctx: Context): RecordingDatabase {
        return RecordingDatabase.getInstance(ctx)
    }

    @Provides
    @Singleton
    fun provideDao(db: RecordingDatabase): RecordingDao {
        return db.recordingDao()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}
