package com.cs250.kratos.data

import com.cs250.kratos.model.Message
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.tasks.await
import java.util.UUID
/**
 * Repository class responsible for managing chat data operations with Firebase Firestore.
 * This class handles sending messages, syncing real-time message history,
 * and calculating unread message counts.
 *
 * @property db The Firestore instance to use for database operations.
 */
class ChatRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {
    /**
     * Generates a canonical Chat ID for a 1-on-1 conversation between two users.
     *
     * It sorts the user IDs alphabetically before joining them. This ensures that
     * "UserA" and "UserB" always generate the same Chat ID (e.g., "UserA_UserB")
     * regardless of who is the sender or receiver.
     *
     * @param u1 The ID of the first user.
     * @param u2 The ID of the second user.
     * @return A unique string identifier for the chat.
     */
    fun chatIdFor(u1: String, u2: String): String =
        listOf(u1, u2).sorted().joinToString("_")
    /**
     * Sends a new message to a specific chat and updates the chat's metadata.
     *
     * This performs two write operations:
     * 1. Adds the message document to the 'messages' subcollection.
     * 2. Updates the parent 'chats' document with the participant list and the timestamp
     * of the last message (used for sorting chats in the main list).
     *
     * @param chatId The ID of the chat.
     * @param senderUid The ID of the user sending the message.
     * @param text The content of the message.
     */
    suspend fun sendMessage(chatId: String, senderUid: String, text: String) {
        val id = UUID.randomUUID().toString()
        val msg = Message(id = id, chatId = chatId, senderUid = senderUid, text = text)
// 1. Write the actual message
        db.collection("chats").document(chatId)
            .collection("messages").document(id)
            .set(msg)
            .await() // Suspend until the write is complete
// 2. Update the chat summary (participants and last activity time)
        // SetOptions.merge() is used to ensure we don't overwrite other fields (like lastRead)
        db.collection("chats").document(chatId)
            .set(
                mapOf(
                    "participants" to chatId.split("_"),
                    "lastAt" to msg.sentAt
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun updateLastReadTimestamp(chatId: String, userId: String) {
        val timestamp = FieldValue.serverTimestamp()
        db.collection("chats").document(chatId)
            .set(mapOf("lastRead" to mapOf(userId to timestamp)), SetOptions.merge())
            .await()
    }

    private fun lastReadTimestampFlow(chatId: String, userId: String): Flow<Long> = callbackFlow {
        val docRef = db.collection("chats").document(chatId)
        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val timestamp = snapshot?.get("lastRead.$userId") as? com.google.firebase.Timestamp
            trySend(timestamp?.toDate()?.time ?: 0L)
        }
        awaitClose { listener.remove() }
    }
    /**
     * Returns a real-time Flow of the number of unread messages for the current user.
     *
     * Logic:
     * 1. Observes the user's 'lastRead' timestamp.
     * 2. Uses `flatMapLatest` to switch to a new query whenever that timestamp changes.
     * 3. The new query counts messages sent by the *other* user *after* the 'lastRead' time.
     *
     * @param currentUserId The user looking at the app.
     * @param otherUserId The person they are chatting with.
     * @return A Flow emitting the integer count of unread messages.
     */
    fun getUnreadMessagesCountForUserFlow(currentUserId: String, otherUserId: String): Flow<Int> {
        val chatId = chatIdFor(currentUserId, otherUserId)

        return lastReadTimestampFlow(chatId, currentUserId).flatMapLatest { lastReadTimestamp ->
            callbackFlow {
                val query = db.collection("chats").document(chatId)
                    .collection("messages")
                    .whereEqualTo("senderUid", otherUserId)
                    .whereGreaterThan("sentAt", lastReadTimestamp)

                val listener = query.addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        close(error)
                        return@addSnapshotListener
                    }
                    // Convert Firestore documents to Kotlin data objects
                    trySend(snapshot?.size() ?: 0)
                }
                // Ensure the Firestore listener is removed when the Flow collection stops
                awaitClose { listener.remove() }
            }
        }
    }


    fun messagesFlow(chatId: String) = callbackFlow<List<Message>> {
        val reg = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("sentAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    close(err)
                    return@addSnapshotListener
                }

                trySend(snap?.toObjects(Message::class.java) ?: emptyList())
            }

        awaitClose { reg.remove() }
    }
}