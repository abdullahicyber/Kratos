package com.cs250.kratos.model

data class Message(
    val id: String = "",
    val chatId: String = "",
    val text: String = "",
    val senderUid: String = "",
    val attachmentUrl: String? = null,
    val sentAt: Long = System.currentTimeMillis()
)