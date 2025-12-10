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

class ChatRepository(private val db: FirebaseFirestore = FirebaseFirestore.getInstance()) {

    fun chatIdFor(u1: String, u2: String): String =
        listOf(u1, u2).sorted().joinToString("_")

    suspend fun sendMessage(chatId: String, senderUid: String, text: String) {
        val id = UUID.randomUUID().toString()
        val msg = Message(id = id, chatId = chatId, senderUid = senderUid, text = text)

        db.collection("chats").document(chatId)
            .collection("messages").document(id)
            .set(msg)
            .await()

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
                    trySend(snapshot?.size() ?: 0)
                }
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