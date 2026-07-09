package com.breakinschmidt.kappariwear.network

import com.breakinschmidt.kappariwear.data.AuthManager
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object PaprikaApiClient {
    private const val BASE_URL = "https://www.paprikaapp.com/"
    
    private var authManager: AuthManager? = null
    
    fun initialize(manager: AuthManager) {
        authManager = manager
    }

    val api: PaprikaApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        
        val authenticator = Authenticator { _, response ->
            if (response.request.url.encodedPath.contains("/api/v1/account/login/")) {
                return@Authenticator null // Don't intercept login failures
            }
            
            val manager = authManager ?: return@Authenticator null
            val email = manager.getEmail() ?: return@Authenticator null
            val password = manager.getPassword() ?: return@Authenticator null
            
            val loginCall = runBlocking {
                try {
                    api.login(email, password)
                } catch (e: Exception) {
                    null
                }
            }
            
            val newToken = loginCall?.result?.token
            if (newToken != null) {
                runBlocking {
                    manager.saveToken(newToken)
                }
                return@Authenticator response.request.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
            }
            
            null
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .authenticator(authenticator)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "Paprika Recipe Manager 3/3.3.1 (Microsoft Windows NT 10.0.26100.0)")
                    .build()
                chain.proceed(request)
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PaprikaApi::class.java)
    }
}
