package com.example.stttts.data.model

data class MessageRequest (
//    val model: String = "claude-3-opus-20240229",
    val model: String = "claude-3-5-sonnet-20241022",
    val max_tokens:Int = 1024,
    val messages: List<Message>
)

data class Message(
    val role: String,
    val content: String
)