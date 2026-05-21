package com.recorder.app.data.model

data class Recording(
    val id: Long = 0,
    val title: String,
    val filePath: String,
    val driveFileId: String? = null,
    val durationMs: Long,
    val createdAt: Long,
    val chords: List<TimedChord> = emptyList(),
    val lyrics: String = "",
    val syncStatus: SyncStatus = SyncStatus.NOT_SYNCED
)

enum class SyncStatus { NOT_SYNCED, SYNCING, SYNCED, ERROR }
