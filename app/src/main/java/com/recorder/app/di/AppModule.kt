package com.recorder.app.di

import android.content.Context
import com.google.gson.Gson
import com.recorder.app.audio.AudioRecorder
import com.recorder.app.audio.ChordAnalyzer
import com.recorder.app.audio.ChordTemplateManager
import com.recorder.app.audio.ChromagramExtractor
import com.recorder.app.audio.LyricsTranscriber
import com.recorder.app.data.db.RecordingDao
import com.recorder.app.data.repository.RecordingRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAudioRecorder(@ApplicationContext ctx: Context): AudioRecorder {
        return AudioRecorder(ctx)
    }

    @Provides
    @Singleton
    fun provideChromagramExtractor(): ChromagramExtractor {
        return ChromagramExtractor()
    }

    @Provides
    @Singleton
    fun provideChordTemplateManager(
        @ApplicationContext ctx: Context,
        extractor: ChromagramExtractor,
        gson: Gson
    ): ChordTemplateManager {
        return ChordTemplateManager(ctx, extractor, gson)
    }

    @Provides
    @Singleton
    fun provideChordAnalyzer(
        extractor: ChromagramExtractor,
        templateManager: ChordTemplateManager
    ): ChordAnalyzer {
        return ChordAnalyzer(extractor, templateManager)
    }

    @Provides
    @Singleton
    fun provideLyricsTranscriber(@ApplicationContext ctx: Context): LyricsTranscriber {
        return LyricsTranscriber(ctx)
    }

    @Provides
    @Singleton
    fun provideRecordingRepository(dao: RecordingDao, gson: Gson): RecordingRepository {
        return RecordingRepository(dao, gson)
    }
}
