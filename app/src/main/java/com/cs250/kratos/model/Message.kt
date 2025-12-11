package com.cs250.kratos.model
/**
 * Represents a single message within a chat conversation.
 *
 * This data class is designed to be compatible with Firestore serialization.
 * It contains all necessary metadata for displaying a message, including
 * content, sender identity, and timing.
 *
 * @property id The unique identifier for this specific message document.
 * @property chatId The ID of the chat (conversation) this message belongs to.
 * @property text The actual text content of the message.
 * @property senderUid The unique user ID (UID) of the person who sent the message.
 * @property attachmentUrl An optional URL pointing to an image or file attachment (null if text-only).
 * @property sentAt The timestamp (in milliseconds) when the message was created. Defaults to the current system time.
 */
data class Message(
    val id: String = "",
    val chatId: String = "",
    val text: String = "",
    val senderUid: String = "",
    val attachmentUrl: String? = null,
    val sentAt: Long = System.currentTimeMillis()
)