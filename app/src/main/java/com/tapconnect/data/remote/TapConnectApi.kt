package com.tapconnect.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import java.util.UUID

interface TapConnectApi {
    @GET("/")
    suspend fun checkStatus(): Map<String, String>

    // --- Auth ---
    @POST("/auth/register")
    suspend fun register(@Body request: AuthRegisterDto): AuthRegisterResponseDto

    @POST("/auth/login")
    suspend fun login(@Body request: AuthLoginDto): AuthTokenResponseDto

    // --- Users ---
    @GET("/users/me")
    suspend fun getMyProfile(): UserProfileDto

    @GET("/users/{user_id}")
    suspend fun getUserProfile(@Path("user_id") userId: UUID): UserProfileDto

    @retrofit2.http.PUT("/users/me")
    suspend fun updateProfile(@Body request: UserProfileUpdateDto): UserProfileDto

    @retrofit2.http.Multipart
    @retrofit2.http.POST("/users/me/profile-image")
    suspend fun uploadProfileImage(
        @retrofit2.http.Part file: okhttp3.MultipartBody.Part
    ): UserProfileDto

    
    // --- Connections ---
    @POST("/connections/request")
    suspend fun sendConnectionRequest(@Body request: ConnectionRequestDto): ConnectionResponseDto

    @POST("/connections/accept")
    suspend fun acceptConnection(@Body action: ConnectionActionDto): ConnectionResponseDto

    @POST("/connections/decline")
    suspend fun declineConnection(@Body action: ConnectionActionDto): ConnectionResponseDto

    @GET("/connections/")
    suspend fun listConnections(): List<ConnectionResponseDto>

    @GET("/connections/pending")
    suspend fun listPendingRequests(): List<ConnectionResponseDto>

    // --- Discovery ---
    @POST("/discovery/update-location")
    suspend fun updateLocation(@Body request: LocationUpdateDto): Map<String, String>

    @GET("/discovery/nearby")
    suspend fun getNearbyUsers(): NearbyUsersResponse
}

data class UserProfileDto(
    val user_id: UUID? = null,
    val email: String,
    val full_name: String,
    val bio: String? = null,
    val role: String? = null,
    val organization: String? = null,
    val profile_image_url: String? = null,
    val interests: List<String> = emptyList(),
    val social_links: Map<String, String> = emptyMap()
)

data class UserProfileUpdateDto(
    val full_name: String? = null,
    val bio: String? = null,
    val role: String? = null,
    val organization: String? = null,
    val interests: List<String>? = null,
    val social_links: Map<String, String>? = null
)


data class ConnectionRequestDto(
    val peer_id: UUID,
    val connection_type: String,
    val where_met: String? = null
)

data class ConnectionActionDto(
    val connection_id: UUID
)

data class ConnectionResponseDto(
    val connection_id: UUID,
    val user_id: UUID,
    val peer_id: UUID,
    val status: String,
    val connection_type: String,
    val peer_name: String? = null,
    val peer_role: String? = null,
    val peer_organization: String? = null,
    val peer_image_url: String? = null,
    val ai_summary: String? = null,
    val where_met: String? = null
)

data class LocationUpdateDto(
    val latitude: Double,
    val longitude: Double,
    val is_networking_active: Boolean
)

data class RadarUserDto(
    val user_id: UUID,
    val full_name: String,
    val role: String?,
    val profile_image_url: String?,
    val distance: Double,
    val angle: Double,
    val actual_distance_km: Double
)

data class NearbyUsersResponse(
    val users: List<RadarUserDto>
)

data class AuthRegisterDto(
    val email: String,
    val password: String,
    val full_name: String
)

data class AuthLoginDto(
    val email: String,
    val password: String
)

data class AuthRegisterResponseDto(
    val user_id: UUID,
    val email: String,
    val full_name: String,
    val access_token: String
)

data class AuthTokenResponseDto(
    val access_token: String,
    val token_type: String = "bearer"
)
