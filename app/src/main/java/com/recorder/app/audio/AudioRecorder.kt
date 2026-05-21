package com.recorder.app.audio

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioRecorder @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFilePath: String? = null

    var isRecording: Boolean = false
        private set

    fun startRecording(outputFile: File) {
        if (isRecording) return

        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        recorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioSamplingRate(44100)
            setAudioEncodingBitRate(128000)
            setOutputFile(outputFile.absolutePath)
        }

        try {
            recorder.prepare()
        } catch (e: Exception) {
            recorder.release()
            return
        }

        recorder.start()
        mediaRecorder = recorder
        outputFilePath = outputFile.absolutePath
        isRecording = true
    }

    fun stopRecording(): Long {
        val recorder = mediaRecorder ?: return 0L
        val filePath = outputFilePath

        try {
            recorder.stop()
        } catch (_: Exception) {
        } finally {
            recorder.release()
            mediaRecorder = null
            outputFilePath = null
            isRecording = false
        }

        if (filePath == null) return 0L

        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(filePath)
            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            durationStr?.toLongOrNull() ?: 0L
        } catch (_: Exception) {
            0L
        } finally {
            retriever.release()
        }
    }
}
