package com.tapconnect.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tapconnect.data.ble.BleAdvertiser
import com.tapconnect.data.remote.RetrofitClient
import com.tapconnect.data.remote.ConnectionRequestDto
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

data class UserStats(
    val connectionsCount: Int = 0
)

sealed class IncomingRequest {
    data class NewRequest(val peerName: String, val peerId: String, val connectionId: String) : IncomingRequest()
    object None : IncomingRequest()
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val advertiser = BleAdvertiser(application)

    private val _isLive = MutableStateFlow(true)
    val isLive: StateFlow<Boolean> = _isLive.asStateFlow()

    private val _stats = MutableStateFlow(UserStats())
    val stats: StateFlow<UserStats> = _stats.asStateFlow()

    private val _incomingRequest = MutableStateFlow<IncomingRequest>(IncomingRequest.None)
    val incomingRequest: StateFlow<IncomingRequest> = _incomingRequest.asStateFlow()

    private val _nfcSimulating = MutableStateFlow(false)
    val nfcSimulating: StateFlow<Boolean> = _nfcSimulating.asStateFlow()

    init {
        loadStats()
        listenForIncomingRequests()
        
        // Connect to WebSocket & start advertising if token exists
        val token = com.tapconnect.data.local.TokenManager.getToken()
        if (token != null) {
            com.tapconnect.data.remote.WebSocketManager.getInstance().connect(token)
        }
        
        val uid = com.tapconnect.data.local.TokenManager.getUserId()
        if (uid != null && _isLive.value) {
            advertiser.startAdvertising(uid)
            viewModelScope.launch {
                try {
                    RetrofitClient.api.updateLocation(
                        com.tapconnect.data.remote.LocationUpdateDto(
                            latitude = 37.7749,
                            longitude = -122.4194,
                            is_networking_active = true
                        )
                    )
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Failed to update initial location status", e)
                }
            }
        }
    }

    fun toggleLiveStatus() {
        val nextState = !_isLive.value
        _isLive.value = nextState
        
        viewModelScope.launch {
            try {
                RetrofitClient.api.updateLocation(
                    com.tapconnect.data.remote.LocationUpdateDto(
                        latitude = 37.7749,
                        longitude = -122.4194,
                        is_networking_active = nextState
                    )
                )
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to update visibility on backend", e)
            }
        }

        val uid = com.tapconnect.data.local.TokenManager.getUserId()
        if (uid != null) {
            if (nextState) {
                advertiser.startAdvertising(uid)
            } else {
                advertiser.stopAdvertising()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        advertiser.stopAdvertising()
    }

    fun loadStats() {
        viewModelScope.launch {
            try {
                val connections = RetrofitClient.api.listConnections()
                _stats.value = _stats.value.copy(connectionsCount = connections.size)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Failed to load stats", e)
            }
        }
    }

    fun simulateNfcTap() {
        viewModelScope.launch {
            _nfcSimulating.value = true
            delay(1500) // Simulate hardware interaction time
            _nfcSimulating.value = false
            
            try {
                // Attempt to fetch nearby active users to connect with
                val nearby = RetrofitClient.api.getNearbyUsers()
                val peer = nearby.users.firstOrNull()
                if (peer != null) {
                    RetrofitClient.api.sendConnectionRequest(
                        ConnectionRequestDto(
                            peer_id = peer.user_id,
                            connection_type = "nfc",
                            where_met = "NFC Tap Meeting"
                        )
                    )
                    loadStats()
                } else {
                    // Fallback to a demo connection request so a notification can be verified single-device
                    _incomingRequest.value = IncomingRequest.NewRequest(
                        peerName = "Demo Developer",
                        peerId = "82d7a708-4a83-4117-bfbb-acb7bc0cfb35",
                        connectionId = UUID.randomUUID().toString()
                    )
                }
            } catch (e: Exception) {
                // Fallback demo request on API failure
                _incomingRequest.value = IncomingRequest.NewRequest(
                    peerName = "Demo Developer",
                    peerId = "82d7a708-4a83-4117-bfbb-acb7bc0cfb35",
                    connectionId = UUID.randomUUID().toString()
                )
            }
        }
    }

    private fun listenForIncomingRequests() {
        viewModelScope.launch {
            com.tapconnect.data.remote.WebSocketManager.getInstance().events.collect { event ->
                if (event is com.tapconnect.data.remote.WebSocketEvent.NewConnectionRequest) {
                    _incomingRequest.value = IncomingRequest.NewRequest(
                        peerName = event.senderName,
                        peerId = event.senderId,
                        connectionId = event.connectionId
                    )
                }
            }
        }
    }

    fun acceptIncomingRequest() {
        val currentRequest = _incomingRequest.value
        _incomingRequest.value = IncomingRequest.None
        if (currentRequest is IncomingRequest.NewRequest) {
            viewModelScope.launch {
                try {
                    RetrofitClient.api.acceptConnection(
                        com.tapconnect.data.remote.ConnectionActionDto(
                            connection_id = UUID.fromString(currentRequest.connectionId)
                        )
                    )
                    loadStats() // Refresh count
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Failed to accept connection", e)
                }
            }
        }
    }

    fun declineIncomingRequest() {
        val currentRequest = _incomingRequest.value
        _incomingRequest.value = IncomingRequest.None
        if (currentRequest is IncomingRequest.NewRequest) {
            viewModelScope.launch {
                try {
                    RetrofitClient.api.declineConnection(
                        com.tapconnect.data.remote.ConnectionActionDto(
                            connection_id = UUID.fromString(currentRequest.connectionId)
                        )
                    )
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Failed to decline connection", e)
                }
            }
        }
    }
}
