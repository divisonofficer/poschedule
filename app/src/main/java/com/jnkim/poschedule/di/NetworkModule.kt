package com.jnkim.poschedule.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.jnkim.poschedule.data.ai.GeminiClient
import com.jnkim.poschedule.data.local.AuthTokenManager
import com.jnkim.poschedule.data.remote.api.GenAiApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)  // Increased for vision/multimodal requests
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)

                // Log response body
                val responseBody = response.body
                val source = responseBody?.source()
                source?.request(Long.MAX_VALUE) // Buffer the entire body
                val buffer = source?.buffer

                val responseBodyString = buffer?.clone()?.readUtf8() ?: ""
                android.util.Log.d("OkHttp", "Response from ${request.url}: $responseBodyString")

                response
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideGenAiApi(okHttpClient: OkHttpClient, json: Json): GenAiApi {
        val contentType = "application/json".toMediaType()
        return Retrofit.Builder()
            .baseUrl("https://genai.postech.ac.kr/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(GenAiApi::class.java)
    }

    @Provides
    @Singleton
    fun provideGeminiClient(tokenManager: AuthTokenManager): GeminiClient {
        return GeminiClient(tokenManager)
    }
}
