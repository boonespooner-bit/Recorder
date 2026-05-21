package com.recorder.app.ui.screens.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recorder.app.data.model.Recording
import com.recorder.app.data.repository.DriveSync
import com.recorder.app.data.repository.RecordingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Ready(val recording: Recording) : DetailUiState()
    object NotFound : DetailUiState()
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: RecordingRepository,
    private val driveSync: DriveSync
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun load(recordingId: Long) {
        viewModelScope.launch {
            val recording = repository.getRecording(recordingId)
            _uiState.value = if (recording != null) {
                DetailUiState.Ready(recording)
            } else {
                DetailUiState.NotFound
            }
        }
    }

    fun updateTitle(newTitle: String) {
        val current = (_uiState.value as? DetailUiState.Ready)?.recording ?: return
        viewModelScope.launch {
            repository.updateTitle(current.id, newTitle)
            _uiState.value = DetailUiState.Ready(current.copy(title = newTitle))
        }
    }

    fun syncToDrive() {
        val current = (_uiState.value as? DetailUiState.Ready)?.recording ?: return
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                driveSync.uploadRecording(current)
                // Reload to pick up updated syncStatus
                val refreshed = repository.getRecording(current.id)
                if (refreshed != null) _uiState.value = DetailUiState.Ready(refreshed)
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
