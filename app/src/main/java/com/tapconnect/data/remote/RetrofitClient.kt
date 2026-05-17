package com.tapconnect.data.remote

import okhttp3.OkHttpClient
import okhttp3.Interceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // Local Development URL Configurations:
    // - For Android Emulator: "http://10.0.2.2:8000/"
    // - For Physical Device (Wi-Fi): "http://192.168.0.101:8000/"
    private const val BASE_URL = "http://192.168.0.101:8000/"
    
    // Retrieve token dynamically from secure persistent storage
    val currentToken: String?
        get() = com.tapconnect.data.local.TokenManager.getToken()

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
        currentToken?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    val api: TapConnectApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TapConnectApi::class.java)
    }
}
