package com.recorder.app.ui.screens.onboarding

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.recorder.app.audio.AudioRecorder
import com.recorder.app.audio.ChordTemplateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val audioRecorder: AudioRecorder,
    private val templateManager: ChordTemplateManager,
    application: Application
) : AndroidViewModel(application) {

    companion object {
        val TRAINING_CHORDS = listOf("G", "C", "D", "A", "Am", "Em", "E", "F")
    }

    private val _currentChordIndex = MutableStateFlow(0)
    val currentChordIndex: StateFlow<Int> = _currentChordIndex.asStateFlow()

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _trainedChords = MutableStateFlow<List<String>>(emptyList())
    val trainedChords: StateFlow<List<String>> = _trainedChords.asStateFlow()

    private val _allDone = MutableStateFlow(false)
    val allDone: StateFlow<Boolean> = _allDone.asStateFlow()

    private var currentTempFile: File? = null

    init {
        viewModelScope.launch {
            _trainedChords.value = templateManager.getTrainedChords()
            if (templateManager.hasAllTemplates()) {
                _allDone.value = true
            }
        }
    }

    fun startRecordingChord() {
        if (_isRecording.value || _isProcessing.value) return
        val chord = TRAINING_CHORDS.getOrNull(_currentChordIndex.value) ?: return

        val cacheDir = getApplication<Application>().cacheDir
        val tempFile = File(cacheDir, "chord_template_${chord}_${System.currentTimeMillis()}.m4a")
        currentTempFile = tempFile

        audioRecorder.startRecording(tempFile)
        _isRecording.value = true
    }

    fun stopAndSaveChord() {
        if (!_isRecording.value) return
        val chord = TRAINING_CHORDS.getOrNull(_currentChordIndex.value) ?: return
        val tempFile = currentTempFile ?: return

        _isProcessing.value = true

        viewModelScope.launch(Dispatchers.IO) {
            audioRecorder.stopRecording()

            withContext(Dispatchers.Main) {
                _isRecording.value = false
            }

            try {
                templateManager.saveTemplate(chord, tempFile)
                withContext(Dispatchers.Main) {
                    val trained = templateManager.getTrainedChords()
                    _trainedChords.value = trained
                    advanceToNextChord()
                }
            } catch (e: Exception) {
                // Continue even if save fails
                withContext(Dispatchers.Main) {
                    advanceToNextChord()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isProcessing.value = false
                }
                tempFile.delete()
                currentTempFile = null
            }
        }
    }

    fun skipChord() {
        if (_isRecording.value) {
            audioRecorder.stopRecording()
            _isRecording.value = false
        }
        currentTempFile?.delete()
        currentTempFile = null
        advanceToNextChord()
    }

    private fun advanceToNextChord() {
        val nextIndex = _currentChordIndex.value + 1
        if (nextIndex >= TRAINING_CHORDS.size) {
            _allDone.value = true
        } else {
            _currentChordIndex.value = nextIndex
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (audioRecorder.isRecording) {
            audioRecorder.stopRecording()
        }
        currentTempFile?.delete()
    }
}
