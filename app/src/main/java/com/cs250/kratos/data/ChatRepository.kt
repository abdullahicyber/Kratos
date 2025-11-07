package com.cs250.kratos.data

import com.cs250.kratos.model.Message
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * # ChatRepository
 *
 * A lightweight Firestore-backed chat repository that handles:
 * - Chat ID generation (consistent between two users)
 * - Sending messages to `chats/{chatId}/messages/{messageId}`
 * - Updating the parent chat metadata (participants, last message timestamp)
 * - Streaming live message updates using Kotlin Flows
 *
 * ## Firestore Structure
 * ```
 * chats/
 *   {chatId}/
 *     participants: [uid1, uid2]
 *     lastAt: <Timestamp of last message>
 *     messages/
 *       {messageId}: {
 *         id, chatId, senderUid, text, sentAt, ...
 *       }
 * ```
 *
 * ## Threading
 * - Suspend functions must be called from a coroutine scope.
 * - [messagesFlow] emits updates on the Firestore snapshot listener thread.
 *
 * @property db Firestore instance (defaults to [FirebaseFirestore.getInstance()])
 */
class ChatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /**
     * Generates a **deterministic chat ID** for a pair of users.
     *
     * This ensures both users will always reference the same chat document,
     * regardless of who initiates the chat first.
     *
     * Example:
     * ```
     * chatIdFor("alice123", "bob456") // => "alice123_bob456"
     * chatIdFor("bob456", "alice123") // => "alice123_bob456"
     * ```
     *
     * @param u1 UID of the first user
     * @param u2 UID of the second user
     * @return a stable chat ID combining both UIDs, alphabetically sorted and joined by `_`
     */
    fun chatIdFor(u1: String, u2: String): String =
        listOf(u1, u2).sorted().joinToString("_")

    /**
     * Sends a new message to the Firestore chat collection.
     *
     * This method:
     * 1. Creates a [Message] object with a unique UUID.
     * 2. Writes it to `chats/{chatId}/messages/{messageId}`.
     * 3. Updates the parent `chats/{chatId}` document with:
     *    - `participants`: a list of the two user UIDs (split from the chatId)
     *    - `lastAt`: timestamp of the latest message.
     *
     * Firestore will automatically create the parent chat document if it doesn't exist.
     *
     * Example:
     * ```kotlin
     * chatRepo.sendMessage(chatId = "alice123_bob456", senderUid = "alice123", text = "Hey there!")
     * ```
     *
     * @param chatId The Firestore chat document ID (use [chatIdFor] to generate one)
     * @param senderUid UID of the sender
     * @param text The message text
     *
     * @throws com.google.firebase.FirebaseException if any Firestore write fails.
     */
    suspend fun sendMessage(chatId: String, senderUid: String, text: String) {
        // Generate a random message ID
        val id = UUID.randomUUID().toString()

        // Create a message data object (your Message model should include sentAt by default)
        val msg = Message(id = id, chatId = chatId, senderUid = senderUid, text = text)

        // Write message to Firestore under chats/{chatId}/messages/{id}
        db.collection("chats").document(chatId)
            .collection("messages").document(id)
            .set(msg)
            .await()

        // Update or create the parent chat document with participants + last message time
        db.collection("chats").document(chatId)
            .set(
                mapOf(
                    "participants" to chatId.split("_"),
                    "lastAt" to msg.sentAt
                ),
                SetOptions.merge() // merge to preserve any other fields
            )
            .await()
    }

    /**
     * Provides a **real-time stream** of messages for a given chat.
     *
     * This uses [callbackFlow] to wrap a Firestore `addSnapshotListener`, automatically
     * emitting a new list of [Message] objects whenever messages are added/changed.
     *
     * Example usage:
     * ```kotlin
     * viewModelScope.launch {
     *   chatRepo.messagesFlow(chatId).collect { messages ->
     *     // Update UI with new messages
     *   }
     * }
     * ```
     *
     * The flow completes when the collector is canceled, at which point the Firestore
     * listener is removed to prevent memory leaks.
     *
     * @param chatId The Firestore chat document ID
     * @return a cold [kotlinx.coroutines.flow.Flow] emitting ordered message lists
     */
    fun messagesFlow(chatId: String) = callbackFlow<List<Message>> {
        // Listen to messages ordered by sentAt ascending (oldest to newest)
        val reg = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    // Close the flow on Firestore listener error
                    close(err)
                    return@addSnapshotListener
                }

                // Convert snapshot to list of Message objects, or empty list if null
                trySend(snap?.toObjects(Message::class.java) ?: emptyList())
            }

        // Clean up listener when flow collector is canceled
        awaitClose { reg.remove() }
    }
}
