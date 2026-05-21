package com.recorder.app.ui.screens.signin

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class SignInUiState {
    object Loading : SignInUiState()
    object SignedOut : SignInUiState()
    object SignedIn : SignInUiState()
}

class SignInViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<SignInUiState>(SignInUiState.SignedOut)
    val uiState: StateFlow<SignInUiState> = _uiState.asStateFlow()

    fun setLoading() {
        _uiState.value = SignInUiState.Loading
    }

    fun setSignedIn() {
        _uiState.value = SignInUiState.SignedIn
    }

    fun setSignedOut() {
        _uiState.value = SignInUiState.SignedOut
    }
}
