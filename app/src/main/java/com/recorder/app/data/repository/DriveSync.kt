package com.recorder.app.data.repository

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.recorder.app.data.model.Recording
import com.recorder.app.data.model.SyncStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DriveSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: RecordingRepository
) {

    suspend fun uploadRecording(recording: Recording) {
        withContext(Dispatchers.IO) {
            val account = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext

            repository.updateSyncStatus(recording.id, SyncStatus.SYNCING)

            try {
                val credential = GoogleAccountCredential.usingOAuth2(
                    context,
                    listOf(DriveScopes.DRIVE_FILE)
                ).apply {
                    selectedAccount = account.account
                }

                val drive = Drive.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential
                )
                    .setApplicationName("Recorder")
                    .build()

                val audioFile = File(recording.filePath)
                val filename = audioFile.name

                val fileMetadata = com.google.api.services.drive.model.File().apply {
                    name = filename
                    parents = listOf("root")
                }

                val mediaContent = FileContent("audio/mp4", audioFile)

                val driveFile = drive.files()
                    .create(fileMetadata, mediaContent)
                    .setFields("id")
                    .execute()

                repository.updateDriveFileId(recording.id, driveFile.id, SyncStatus.SYNCED)
            } catch (_: Exception) {
                repository.updateSyncStatus(recording.id, SyncStatus.ERROR)
            }
        }
    }

    suspend fun syncAll() {
        withContext(Dispatchers.IO) {
            val allRecordings = repository.getAllRecordingsSnapshot()
            val unsynced = allRecordings.filter { it.syncStatus != SyncStatus.SYNCED }
            for (recording in unsynced) {
                uploadRecording(recording)
            }
        }
    }
}
