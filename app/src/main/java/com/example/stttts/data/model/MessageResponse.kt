package com.example.stttts.data.model

data class MessageResponse (
    val content: List<Content>
)

data class Content(
    val text: String
)