package com.tapconnect.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tapconnect.data.ble.BleScanner
import com.tapconnect.data.remote.RetrofitClient
import com.tapconnect.data.remote.UserProfileDto
import com.tapconnect.data.remote.ConnectionRequestDto
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

sealed class ConnectionStatus {
    object Idle : ConnectionStatus()
    object Sending : ConnectionStatus()
    object Connected : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

class DiscoveryViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner = BleScanner(application)
    
    val discoveredUsers: StateFlow<Set<String>> = scanner.discoveredUsers
    
    private val _discoveredProfiles = MutableStateFlow<Map<String, UserProfileDto>>(emptyMap())
    val discoveredProfiles: StateFlow<Map<String, UserProfileDto>> = _discoveredProfiles.asStateFlow()
    
    private val _connectionStatus = MutableStateFlow<Map<String, ConnectionStatus>>(emptyMap())
    val connectionStatus: StateFlow<Map<String, ConnectionStatus>> = _connectionStatus.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val currentUserId: String?
        get() = com.tapconnect.data.local.TokenManager.getUserId()

    private var networkJob: Job? = null

    init {
        viewModelScope.launch {
            discoveredUsers.collect { userIds ->
                userIds.forEach { userId ->
                    if (!_discoveredProfiles.value.containsKey(userId)) {
                        fetchUserProfile(userId)
                    }
                }
            }
        }
    }

    fun fetchUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                if (userId == currentUserId) return@launch

                val uuid = try {
                    UUID.fromString(userId)
                } catch (e: Exception) {
                    Log.e("DiscoveryViewModel", "Invalid UUID discovered: $userId")
                    return@launch
                }

                val profile = RetrofitClient.api.getUserProfile(uuid)
                val nameLower = profile.full_name.lowercase().trim()
                val isMock = nameLower.contains("user a") || 
                             nameLower.contains("user b") || 
                             nameLower == "ua" || 
                             nameLower == "ub"
                             
                if (!isMock) {
                    _discoveredProfiles.value += (userId to profile)
                } else {
                    Log.d("DiscoveryViewModel", "Filtered out mock user profile: ${profile.full_name}")
                }
            } catch (e: Exception) {
                Log.e("DiscoveryViewModel", "Error fetching profile for $userId", e)
            }
        }
    }

    fun sendConnectionRequest(peerId: String, whereMet: String? = null) {
        viewModelScope.launch {
            try {
                _connectionStatus.value += (peerId to ConnectionStatus.Sending)

                val peerUuid = UUID.fromString(peerId)
                RetrofitClient.api.sendConnectionRequest(
                    ConnectionRequestDto(
                        peer_id = peerUuid,
                        connection_type = "radar",
                        where_met = whereMet
                    )
                )

                _connectionStatus.value += (peerId to ConnectionStatus.Connected)
            } catch (e: Exception) {
                Log.e("DiscoveryViewModel", "Failed to send request to $peerId", e)
                _connectionStatus.value += (peerId to ConnectionStatus.Error("Failed to send request"))
            }
        }
    }

    fun startDiscovery() {
        Log.d("DiscoveryViewModel", "Starting Radar scan")
        _isScanning.value = true
        scanner.startScanning()

        // Start network-based discovery polling
        networkJob?.cancel()
        networkJob = viewModelScope.launch {
            while (_isScanning.value) {
                try {
                    // Update location and networking active status on backend
                    RetrofitClient.api.updateLocation(
                        com.tapconnect.data.remote.LocationUpdateDto(
                            latitude = 37.7749,
                            longitude = -122.4194,
                            is_networking_active = true
                        )
                    )
                    
                    // Fetch nearby users from network
                    val response = RetrofitClient.api.getNearbyUsers()
                    response.users.forEach { radarUser ->
                        val userIdStr = radarUser.user_id.toString()
                        val isMockUser = radarUser.full_name.equals("User A", ignoreCase = true) ||
                                         radarUser.full_name.equals("User B", ignoreCase = true)
                        if (userIdStr != currentUserId && !isMockUser && !_discoveredProfiles.value.containsKey(userIdStr)) {
                            fetchUserProfile(userIdStr)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("DiscoveryViewModel", "Failed to fetch network nearby users", e)
                }
                delay(4000) // Poll every 4 seconds
            }
        }
    }

    fun stopDiscovery() {
        _isScanning.value = false
        scanner.stopScanning()
        networkJob?.cancel()
        networkJob = null
        _discoveredProfiles.value = emptyMap()
        _connectionStatus.value = emptyMap()
    }

    override fun onCleared() {
        super.onCleared()
        stopDiscovery()
    }
}
