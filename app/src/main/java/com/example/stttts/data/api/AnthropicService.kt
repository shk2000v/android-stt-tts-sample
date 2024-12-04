package com.example.stttts.data.api

import com.example.stttts.data.model.MessageRequest
import com.example.stttts.data.model.MessageResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AnthropicService {
    @POST("v1/messages")
    suspend fun sendMessage(
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body requestBody: MessageRequest
    ): MessageResponse
}