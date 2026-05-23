package com.tapconnect.data.remote

import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import okhttp3.*
import org.json.JSONObject

sealed class WebSocketEvent {
    data class NewConnectionRequest(
        val connectionId: String,
        val senderId: String,
        val senderName: String,
        val message: String
    ) : WebSocketEvent()
    
    data class RequestAccepted(
        val connectionId: String,
        val accepterId: String,
        val accepterName: String,
        val message: String
    ) : WebSocketEvent()
}

class WebSocketManager private constructor() {
    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    
    private val _events = MutableSharedFlow<WebSocketEvent>(extraBufferCapacity = 10)
    val events: SharedFlow<WebSocketEvent> = _events
    
    // Replace with your local IP if testing on physical device
    private val WS_BASE_URL = "https://tap-connect-qni6.onrender.com/"

    companion object {
        @Volatile
        private var instance: WebSocketManager? = null

        fun getInstance(): WebSocketManager {
            return instance ?: synchronized(this) {
                instance ?: WebSocketManager().also { instance = it }
            }
        }
    }

    fun connect(token: String) {
        if (webSocket != null) return // Already connected
        
        val request = Request.Builder()
            .url("$WS_BASE_URL/ws/notifications?token=$token")
            .build()
            
        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d("WebSocketManager", "Connected to WS")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d("WebSocketManager", "Message received: $text")
                try {
                    val json = JSONObject(text)
                    when (json.optString("type")) {
                        "new_connection_request" -> {
                            _events.tryEmit(
                                WebSocketEvent.NewConnectionRequest(
                                    connectionId = json.getString("connection_id"),
                                    senderId = json.getString("sender_id"),
                                    senderName = json.getString("sender_name"),
                                    message = json.getString("message")
                                )
                            )
                        }
                        "request_accepted" -> {
                            _events.tryEmit(
                                WebSocketEvent.RequestAccepted(
                                    connectionId = json.getString("connection_id"),
                                    accepterId = json.getString("accepter_id"),
                                    accepterName = json.getString("accepter_name"),
                                    message = json.getString("message")
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("WebSocketManager", "Error parsing message", e)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e("WebSocketManager", "WS Error: ${t.message}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d("WebSocketManager", "WS Closed")
            }
        })
    }
    
    fun disconnect() {
        webSocket?.close(1000, "Normal closure")
        webSocket = null
    }
}
