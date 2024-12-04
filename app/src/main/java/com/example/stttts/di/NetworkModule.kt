package com.example.stttts.di

import android.content.Context
import android.util.Log
import com.example.stttts.BuildConfig
import com.example.stttts.data.api.AnthropicService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object NetworkModule {
    private const val BASE_URL = "https://api.anthropic.com/"

    fun createAnthropicService(context: Context): AnthropicService {
        val apiKey = BuildConfig.ANTHROPIC_API_KEY

        Log.d("NetworkModule", "Api Key loaded : ${apiKey.isNotEmpty()}")
        Log.d("NetworkModule", "API Key length: ${apiKey.length}")
        // API 키의 처음 몇 글자만 로깅 (보안을 위해)
        Log.d("NetworkModule", "API Key starts with: ${apiKey.take(10)}...")

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val original = chain.request()
                val request = original.newBuilder()
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .header("content-type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AnthropicService::class.java)
    }
}