package com.recorder.app.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.recorder.app.data.model.Recording
import com.recorder.app.data.model.SyncStatus
import com.recorder.app.data.model.TimedChord

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val driveFileId: String?,
    val durationMs: Long,
    val createdAt: Long,
    val chordsJson: String,
    val lyrics: String,
    val syncStatus: String
) {
    fun toRecording(gson: Gson): Recording {
        val chordsType = object : TypeToken<List<TimedChord>>() {}.type
        val chords: List<TimedChord> = gson.fromJson(chordsJson, chordsType) ?: emptyList()
        return Recording(
            id = id,
            title = title,
            filePath = filePath,
            driveFileId = driveFileId,
            durationMs = durationMs,
            createdAt = createdAt,
            chords = chords,
            lyrics = lyrics,
            syncStatus = SyncStatus.valueOf(syncStatus)
        )
    }
}

fun Recording.toEntity(gson: Gson): RecordingEntity {
    return RecordingEntity(
        id = id,
        title = title,
        filePath = filePath,
        driveFileId = driveFileId,
        durationMs = durationMs,
        createdAt = createdAt,
        chordsJson = gson.toJson(chords),
        lyrics = lyrics,
        syncStatus = syncStatus.name
    )
}
