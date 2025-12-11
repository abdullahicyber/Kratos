package com.cs250.kratos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.cs250.kratos.data.ChatRepository
import com.cs250.kratos.databinding.ActivityChatBinding
import com.cs250.kratos.databinding.ItemMessageReceivedBinding
import com.cs250.kratos.databinding.ItemMessageSentBinding
import com.cs250.kratos.model.Message
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.text.SimpleDateFormat
import androidx.core.widget.doOnTextChanged
import com.cs250.kratos.utils.FcmApiHelper
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*
/**
 * The main activity for the 1-on-1 chat screen.
 *
 * Responsibilities:
 * 1. Validating the current user and the recipient (other user).
 * 2. Displaying the list of messages in real-time.
 * 3. Handling message sending and triggering push notifications.
 * 4. Updating the "read" status for the conversation.
 */
class ChatActivity : AppCompatActivity() {

    companion object { // Key used to pass the recipient's User ID via Intent
        const val EXTRA_OTHER_UID = "otherUid" }

    private lateinit var binding: ActivityChatBinding
    private val repo = ChatRepository()
    private lateinit var myUid: String
    private lateinit var chatId: String
    private lateinit var adapter: MessagesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(
                Intent(this, SignInActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            finish()
            return
        }
        myUid = currentUser.uid
// Initialize UI and Adapter
        adapter = MessagesAdapter(myUid)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)
// 2. Input Logic: Disable send button if the text box is empty
        binding.send.isEnabled = false
        binding.input.doOnTextChanged { text, _, _, _ ->
            binding.send.isEnabled = !text.isNullOrBlank()
        }
// 3. Recipient Validation: Get the other user's ID from the Intent
        val otherUid = intent.getStringExtra(EXTRA_OTHER_UID)
        if (otherUid == null) {
            finish()
            return
        }
// Prevent chatting with yourself (edge case protection)
        if (otherUid == myUid) {
            finish()
            return
        }
// 4. Setup Chat ID: Generates the unique conversation ID based on both User IDs
        chatId = repo.chatIdFor(myUid, otherUid)
        // Mark chat as read immediately upon entering
        lifecycleScope.launch {
            try {
                repo.updateLastReadTimestamp(chatId, myUid)
            } catch (e: Exception) {
                Log.w("ChatActivity", "Failed to update last read timestamp", e)
            }
        }
// 5. RecyclerView Setup
        binding.messages.layoutManager = LinearLayoutManager(this).apply {
            // Important for chat: start filling items from the bottom up
            stackFromEnd = true
        }
        binding.messages.adapter = adapter
// 6. Observe Messages: Listen to the Flow from Repository
        repo.messagesFlow(chatId)
            .onEach { list ->
                adapter.submit(list)
                if (list.isNotEmpty()) {
                    binding.messages.scrollToPosition(adapter.itemCount - 1)
                }
            }
            .launchIn(lifecycleScope) // Auto-cancels when Activity is destroyed
// 7. Send Button Logic
        binding.send.setOnClickListener {
            val text = binding.input.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
// Disable button temporarily to prevent double-sends            binding.send.isEnabled = false
            lifecycleScope.launch {
                // runCatching safely handles success/failure without crashing
                runCatching { repo.sendMessage(chatId, myUid, text) }
                    .onSuccess {
                        // Reset UI on success
                        binding.input.setText("")
                        binding.send.isEnabled = false
                        binding.messages.scrollToPosition(adapter.itemCount - 1)
// 8. Notification Logic: Fetch recipient's FCM token and send push notif
                        // Note: In production, this logic is often better handled by a Cloud Function backend
                        try {
                            val db = FirebaseFirestore.getInstance()
                            val document = db.collection("users").document(otherUid).get().await()
                            val token = document.getString("fcmToken")
                            if (token != null) {
                                FcmApiHelper.sendNotification(token, "New Message", text)
                            }
                        } catch (e: Exception) {
                            Log.w("ChatActivity", "Failed to send notification", e)
                        }
                    }
                    .onFailure { e ->
                        android.widget.Toast.makeText(
                            this@ChatActivity,
                            "Send failed: ${e.message}",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        binding.send.isEnabled = true
                    }
            }
        }
    }
}
// Constants to define which layout to inflate (Sent vs Received)
private const val VIEW_TYPE_SENT = 1
private const val VIEW_TYPE_RECEIVED = 2
/**
 * RecyclerView Adapter handling the display of chat messages.
 * Uses ViewTypes to distinguish between messages sent by the user and messages received.
 */
private class MessagesAdapter(private val myUid: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Message>()

    fun submit(newItems: List<Message>) {
        items.clear()
        items.addAll(newItems)
        // Note: For better performance in large lists, consider using DiffUtil instead of notifyDataSetChanged
        notifyDataSetChanged()
    }
    /**
     * Determines which layout to use based on the sender's UID.
     * Returns 1 (SENT) if sender is me, 2 (RECEIVED) otherwise.
     */
    override fun getItemViewType(position: Int): Int {
        return if (items[position].senderUid == myUid) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SENT) {
            // Inflate "Bubble on the right" layout
            val binding = ItemMessageSentBinding.inflate(inflater, parent, false)
            SentMessageVH(binding)
        } else {
            // Inflate "Bubble on the left" layout
            val binding = ItemMessageReceivedBinding.inflate(inflater, parent, false)
            ReceivedMessageVH(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = items[position]
        if (holder is SentMessageVH) holder.bind(message)
        else if (holder is ReceivedMessageVH) holder.bind(message)
    }

    override fun getItemCount() = items.size
}
/**
 * ViewHolder for messages sent by the current user (Right aligned).
 */
private class SentMessageVH(private val binding: ItemMessageSentBinding) :
    RecyclerView.ViewHolder(binding.root) {

    private val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun bind(m: Message) {
        binding.msgText.text = m.text
        binding.time.text = fmt.format(Date(m.sentAt))
    }
}
/**
 * ViewHolder for messages received from the other user (Left aligned).
 */
private class ReceivedMessageVH(private val binding: ItemMessageReceivedBinding) :
    RecyclerView.ViewHolder(binding.root) {

    private val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun bind(m: Message) {
        binding.msgText.text = m.text
        binding.time.text = fmt.format(Date(m.sentAt))
    }
}
