package com.tapconnect.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tapconnect.data.remote.RetrofitClient
import com.tapconnect.data.remote.UserProfileDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        fetchProfile()
    }

    fun fetchProfile() {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Loading
            try {
                val profile = RetrofitClient.api.getMyProfile()
                profile.user_id?.let {
                    com.tapconnect.data.local.TokenManager.saveUserId(it.toString())
                }
                _uiState.value = ProfileUiState.Success(profile)
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Failed to fetch profile")
            }
        }
    }

    fun updateProfile(update: com.tapconnect.data.remote.UserProfileUpdateDto, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val updatedProfile = RetrofitClient.api.updateProfile(update)
                updatedProfile.user_id?.let {
                    com.tapconnect.data.local.TokenManager.saveUserId(it.toString())
                }
                _uiState.value = ProfileUiState.Success(updatedProfile)
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun uploadProfileImage(filePart: okhttp3.MultipartBody.Part, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = ProfileUiState.Updating
            try {
                val updatedProfile = RetrofitClient.api.uploadProfileImage(filePart)
                updatedProfile.user_id?.let {
                    com.tapconnect.data.local.TokenManager.saveUserId(it.toString())
                }
                _uiState.value = ProfileUiState.Success(updatedProfile)
                onComplete(true)
            } catch (e: Exception) {
                _uiState.value = ProfileUiState.Error(e.message ?: "Failed to upload image")
                onComplete(false)
            }
        }
    }
}

sealed class ProfileUiState {
    object Loading : ProfileUiState()
    object Updating : ProfileUiState()
    data class Success(val profile: UserProfileDto) : ProfileUiState()
    data class Error(val message: String) : ProfileUiState()
}

