package com.recorder.app.ui.screens.record

import android.app.Application
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.recorder.app.audio.AudioRecorder
import com.recorder.app.audio.ChordAnalyzer
import com.recorder.app.audio.LyricsTranscriber
import com.recorder.app.data.model.Recording
import com.recorder.app.data.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed class RecordState {
    object Idle : RecordState()
    object Recording : RecordState()
    object Saving : RecordState()
    data class Done(val recordingId: Long) : RecordState()
}

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val lyricsTranscriber: LyricsTranscriber,
    private val chordAnalyzer: ChordAnalyzer,
    private val repository: RecordingRepository,
    application: Application
) : AndroidViewModel(application) {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _elapsedMs = MutableStateFlow(0L)
    val elapsedMs: StateFlow<Long> = _elapsedMs.asStateFlow()

    private val _partialLyrics = MutableStateFlow("")
    val partialLyrics: StateFlow<String> = _partialLyrics.asStateFlow()

    private val _recordingState = MutableStateFlow<RecordState>(RecordState.Idle)
    val recordingState: StateFlow<RecordState> = _recordingState.asStateFlow()

    private var currentFile: File? = null
    private var timerJob: Job? = null
    private var finalLyrics = StringBuilder()

    fun startRecording() {
        if (_recordingState.value != RecordState.Idle) return

        val musicDir = getApplication<Application>()
            .getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: getApplication<Application>().filesDir

        val timestamp = System.currentTimeMillis()
        val fileName = "recording_$timestamp.m4a"
        val file = File(musicDir, fileName)
        currentFile = file

        audioRecorder.startRecording(file)
        _isRecording.value = true
        _recordingState.value = RecordState.Recording
        _elapsedMs.value = 0L
        finalLyrics.clear()
        _partialLyrics.value = ""

        // Start elapsed timer
        timerJob = viewModelScope.launch {
            while (_isRecording.value) {
                delay(100)
                _elapsedMs.value += 100L
            }
        }

        // Start lyrics transcription
        lyricsTranscriber.startTranscribing(
            onPartialResult = { partial ->
                _partialLyrics.value = buildDisplayLyrics(partial)
            },
            onFinalResult = { final ->
                finalLyrics.append(if (finalLyrics.isNotEmpty()) " $final" else final)
                _partialLyrics.value = buildDisplayLyrics("")
            }
        )
    }

    fun stopAndSave() {
        if (_recordingState.value != RecordState.Recording) return

        _isRecording.value = false
        timerJob?.cancel()
        timerJob = null

        _recordingState.value = RecordState.Saving

        val file = currentFile ?: run {
            _recordingState.value = RecordState.Idle
            return
        }

        viewModelScope.launch {
            val durationMs = audioRecorder.stopRecording()
            lyricsTranscriber.stopTranscribing()

            val titleFormat = SimpleDateFormat("MMM d · h:mm a", Locale.getDefault())
            val title = titleFormat.format(Date())
            val lyrics = finalLyrics.toString().trim()
            val timestamp = System.currentTimeMillis()

            val recording = Recording(
                title = title,
                filePath = file.absolutePath,
                durationMs = durationMs,
                createdAt = timestamp,
                lyrics = lyrics
            )

            val newId = repository.insertRecording(recording)

            // Trigger chord analysis in background after save
            launch {
                try {
                    val chords = chordAnalyzer.analyzeFile(file.absolutePath)
                    repository.updateChordsAndLyrics(newId, chords, lyrics)
                } catch (_: Exception) {
                    // Analysis failure is non-fatal
                }
            }

            _recordingState.value = RecordState.Done(newId)
        }
    }

    private fun buildDisplayLyrics(partial: String): String {
        val base = finalLyrics.toString()
        return if (partial.isNotEmpty()) {
            if (base.isNotEmpty()) "$base $partial" else partial
        } else {
            base
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        if (audioRecorder.isRecording) {
            audioRecorder.stopRecording()
        }
        if (lyricsTranscriber.isTranscribing) {
            lyricsTranscriber.stopTranscribing()
        }
    }
}
