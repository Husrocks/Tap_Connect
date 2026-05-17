package com.tapconnect.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tapconnect.data.remote.RetrofitClient
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ConnectionsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<ConnectionsUiState>(ConnectionsUiState.Loading)
    val uiState: StateFlow<ConnectionsUiState> = _uiState

    // Track which request IDs are currently in-flight for accept or decline actions
    private val _processingRequests = MutableStateFlow<Map<String, String>>(emptyMap()) // connectionId -> "accepting" or "declining"
    val processingRequests: StateFlow<Map<String, String>> = _processingRequests

    init {
        fetchConnectionsAndRequests()
    }

    fun fetchConnectionsAndRequests() {
        viewModelScope.launch {
            _uiState.value = ConnectionsUiState.Loading
            try {
                // Fetch connections and pending requests in parallel
                val connectionsDeferred = async { RetrofitClient.api.listConnections() }
                val pendingDeferred = async { RetrofitClient.api.listPendingRequests() }

                val connections = try {
                    connectionsDeferred.await()
                } catch (_: Exception) {
                    emptyList()
                }

                val pending = try {
                    pendingDeferred.await()
                } catch (_: Exception) {
                    emptyList()
                }

                val connItems = connections.map { dto ->
                    ConnectionItem(
                        id = dto.connection_id.toString(),
                        name = dto.peer_name ?: "User ${dto.peer_id.toString().take(4)}",
                        role = dto.peer_role ?: "Professional",
                        organization = dto.peer_organization ?: "Partner",
                        date = "Recently",
                        aiSummary = dto.ai_summary ?: "Connected via ${dto.connection_type}",
                        whereMet = dto.where_met,
                    )
                }

                val pendItems = pending.map { dto ->
                    PendingRequestItem(
                        connectionId = dto.connection_id.toString(),
                        peerId = dto.peer_id.toString(),
                        name = dto.peer_name ?: "User ${dto.peer_id.toString().take(4)}",
                        role = dto.peer_role ?: "Professional",
                        organization = dto.peer_organization ?: "Partner",
                        aiSummary = dto.ai_summary ?: "Wants to connect via ${dto.connection_type}",
                        date = "Recently",
                        whereMet = dto.where_met,
                    )
                }

                _uiState.value = ConnectionsUiState.Success(connItems, pendItems)
            } catch (e: Exception) {
                _uiState.value = ConnectionsUiState.Error(e.message ?: "Failed to load connection data")
            }
        }
    }

    fun acceptRequest(connectionId: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _processingRequests.value += (connectionId to "accepting")
            try {
                RetrofitClient.api.acceptConnection(
                    com.tapconnect.data.remote.ConnectionActionDto(
                        connection_id = java.util.UUID.fromString(connectionId)
                    )
                )
                // Refresh data to update both tabs instantly
                fetchConnectionsAndRequests()
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Could not accept request. Try again.")
            } finally {
                _processingRequests.value -= connectionId
            }
        }
    }

    fun declineRequest(connectionId: String, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            _processingRequests.value += (connectionId to "declining")
            try {
                RetrofitClient.api.declineConnection(
                    com.tapconnect.data.remote.ConnectionActionDto(
                        connection_id = java.util.UUID.fromString(connectionId)
                    )
                )
                // Refresh data to update both tabs instantly
                fetchConnectionsAndRequests()
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message ?: "Could not decline request. Try again.")
            } finally {
                _processingRequests.value -= connectionId
            }
        }
    }
}

sealed class ConnectionsUiState {
    object Loading : ConnectionsUiState()
    data class Success(
        val connections: List<ConnectionItem>,
        val pendingRequests: List<PendingRequestItem>
    ) : ConnectionsUiState()
    data class Error(val message: String) : ConnectionsUiState()
}
