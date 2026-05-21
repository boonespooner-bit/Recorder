package com.recorder.app.data.repository

import com.google.gson.Gson
import com.recorder.app.data.db.RecordingDao
import com.recorder.app.data.db.entities.toEntity
import com.recorder.app.data.model.Recording
import com.recorder.app.data.model.SyncStatus
import com.recorder.app.data.model.TimedChord
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RecordingRepository @Inject constructor(
    private val dao: RecordingDao,
    private val gson: Gson
) {

    fun getAllRecordings(): Flow<List<Recording>> {
        return dao.getAllRecordings().map { entities ->
            entities.map { it.toRecording(gson) }
        }
    }

    suspend fun getAllRecordingsSnapshot(): List<Recording> {
        return dao.getAllRecordingsSnapshot().map { it.toRecording(gson) }
    }

    suspend fun getRecording(id: Long): Recording? {
        return dao.getRecordingById(id)?.toRecording(gson)
    }

    suspend fun insertRecording(recording: Recording): Long {
        return dao.insert(recording.toEntity(gson))
    }

    suspend fun updateRecording(recording: Recording) {
        dao.update(recording.toEntity(gson))
    }

    suspend fun deleteRecording(recording: Recording) {
        dao.delete(recording.toEntity(gson))
    }

    suspend fun updateTitle(id: Long, title: String) {
        dao.updateTitle(id, title)
    }

    suspend fun updateChordsAndLyrics(id: Long, chords: List<TimedChord>, lyrics: String) {
        dao.updateChordsAndLyrics(id, gson.toJson(chords), lyrics)
    }

    suspend fun updateDriveFileId(id: Long, driveFileId: String, syncStatus: SyncStatus) {
        dao.updateDriveFileId(id, driveFileId, syncStatus.name)
    }

    suspend fun updateSyncStatus(id: Long, syncStatus: SyncStatus) {
        dao.updateSyncStatus(id, syncStatus.name)
    }
}
