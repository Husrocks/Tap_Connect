package com.tapconnect.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tapconnect.data.remote.AuthLoginDto
import com.tapconnect.data.remote.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState

    fun login(email: String, password: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = RetrofitClient.api.login(AuthLoginDto(email, password))
                com.tapconnect.data.local.TokenManager.saveToken(response.access_token)
                _uiState.value = AuthUiState.Success
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Login failed")
            }
        }
    }

    fun register(email: String, password: String, fullName: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = RetrofitClient.api.register(
                    com.tapconnect.data.remote.AuthRegisterDto(email, password, fullName)
                )
                com.tapconnect.data.local.TokenManager.saveToken(response.access_token)
                com.tapconnect.data.local.TokenManager.saveUserId(response.user_id.toString())
                _uiState.value = AuthUiState.Success
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.message ?: "Registration failed")
            }
        }
    }
}

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}
