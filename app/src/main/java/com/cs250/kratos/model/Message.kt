package com.cs250.kratos.model

/**
 * # Message
 *
 * Represents a single chat message stored under:
 * ```
 * chats/{chatId}/messages/{messageId}
 * ```
 *
 * Each message belongs to exactly one chat (identified by [chatId])
 * and is written to Firestore by [ChatRepository.sendMessage].
 *
 * ## Firestore Mapping
 * When stored in Firestore, each message document will have:
 * ```
 * id: "uuid-string",
 * chatId: "uid1_uid2",
 * senderUid: "uid1",
 * text: "Hello!",
 * attachmentUrl: "https://example.com/photo.jpg",
 * sentAt: 1717000000000
 * ```
 *
 * Firestore automatically converts this data class to/from a document
 * when using `toObjects(Message::class.java)` or `.set(msg)`.
 *
 * ## Notes
 * - The default values allow Firebase to deserialize documents even if
 *   some fields are missing.
 * - [sentAt] defaults to the system timestamp at message creation.
 *
 * ## Typical Use
 * ```kotlin
 * val msg = Message(
 *     id = UUID.randomUUID().toString(),
 *     chatId = "alice123_bob456",
 *     senderUid = "alice123",
 *     text = "Hey Bob!"
 * )
 * db.collection("chats").document(msg.chatId)
 *   .collection("messages").document(msg.id)
 *   .set(msg)
 * ```
 *
 * @property id Unique identifier for this message (UUID or Firestore document ID).
 * @property chatId Identifier of the chat thread this message belongs to.
 * @property senderUid UID of the user who sent this message.
 * @property text Optional text content of the message. `null` if this is purely an attachment.
 * @property attachmentUrl Optional URL to an uploaded image, file, or media resource.
 * @property sentAt Epoch time (milliseconds) when the message was created or sent.
 */
data class Message(
    val id: String = "",
    val chatId: String = "",
    val senderUid: String = "",
    val text: String? = null,
    val attachmentUrl: String? = null,
    val sentAt: Long = System.currentTimeMillis()
)
