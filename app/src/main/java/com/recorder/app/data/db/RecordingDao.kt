package com.recorder.app.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.recorder.app.data.db.entities.RecordingEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordingDao {

    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings ORDER BY createdAt DESC")
    suspend fun getAllRecordingsSnapshot(): List<RecordingEntity>

    @Query("SELECT * FROM recordings WHERE id = :id")
    suspend fun getRecordingById(id: Long): RecordingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recording: RecordingEntity): Long

    @Update
    suspend fun update(recording: RecordingEntity)

    @Delete
    suspend fun delete(recording: RecordingEntity)

    @Query("UPDATE recordings SET title = :title WHERE id = :id")
    suspend fun updateTitle(id: Long, title: String)

    @Query("UPDATE recordings SET chordsJson = :chordsJson, lyrics = :lyrics WHERE id = :id")
    suspend fun updateChordsAndLyrics(id: Long, chordsJson: String, lyrics: String)

    @Query("UPDATE recordings SET driveFileId = :driveFileId, syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateDriveFileId(id: Long, driveFileId: String, syncStatus: String)

    @Query("UPDATE recordings SET syncStatus = :syncStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, syncStatus: String)
}
