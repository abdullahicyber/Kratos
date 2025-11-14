package com.cs250.kratos

import android.content.Intent
import android.os.Bundle
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
import kotlinx.coroutines.launch
import java.util.*

/**
 * # ChatActivity
 *
 * A chat screen for a 1:1 conversation between the currently signed-in user
 * and another user. Responsibilities:
 * - **Auth guard**: if no Firebase user is signed in, redirect to [SignInActivity].
 * - **Chat wiring**: compute a stable `chatId` and stream messages via [ChatRepository.messagesFlow].
 * - **UI**: render messages in a RecyclerView, auto-scroll, and handle sending new messages.
 *
 * ## Intent contract
 * Requires an `otherUid` extra (see [EXTRA_OTHER_UID]) to identify the chat partner.
 *
 * ## Data flow
 * - Reads `otherUid` from the intent.
 * - Builds `chatId` using `repo.chatIdFor(myUid, otherUid)`.
 * - Subscribes to `messagesFlow(chatId)` and feeds the adapter.
 * - Sends messages via `repo.sendMessage(chatId, myUid, text)`.
 *
 * ## UI notes
 * - `stackFromEnd = true` so the list grows upward and stays near latest messages.
 * - Send button is enabled only when the input is non-blank.
 */
class ChatActivity : AppCompatActivity() {

    /** Intent extra key for the other participant's UID. */
    companion object { const val EXTRA_OTHER_UID = "otherUid" }

    /** ViewBinding for the Activity layout. */
    private lateinit var binding: ActivityChatBinding

    /** Repository that encapsulates Firestore chat operations. */
    private val repo = ChatRepository()

    /** Current user's UID (set after auth check). */
    private lateinit var myUid: String

    /** Deterministic chat identifier for (myUid, otherUid). */
    private lateinit var chatId: String

    /** RecyclerView adapter for rendering messages. */
    private lateinit var adapter: MessagesAdapter
    /**
     * Standard Activity creation: guard auth, init views, wire streams and send actions.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1) Auth guard: if no user, bounce to sign-in and clear back stack.
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

        // 2) Safe to build UI + adapter now that we know who we are.
        adapter = MessagesAdapter(myUid)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Disable send until user types something non-blank.
        binding.send.isEnabled = false
        binding.input.doOnTextChanged { text, _, _, _ ->
            binding.send.isEnabled = !text.isNullOrBlank()
        }

        // 3) Read chat partner from intent and prevent self-chat.
        val otherUid = intent.getStringExtra(EXTRA_OTHER_UID) ?: return finish()
        if (otherUid == myUid) {
            // Defensive: don't allow chatting with yourself.
            finish()
            return
        }

        // 4) Compute chatId (sorted pair), set up list UI.
        chatId = repo.chatIdFor(myUid, otherUid)
        binding.messages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true // Keep view anchored at the bottom like typical chat apps.
        }
        binding.messages.adapter = adapter

        // 5) Subscribe to message stream; update adapter and scroll to bottom on new items.
        repo.messagesFlow(chatId)
            .onEach { list ->
                adapter.submit(list)
                if (list.isNotEmpty()) {
                    binding.messages.scrollToPosition(adapter.itemCount - 1)
                }
            }
            .launchIn(lifecycleScope) // Tied to Activity lifecycle

        // 6) Send button handler: write message, clear input, re-disable send on success.
        binding.send.setOnClickListener {
            val text = binding.input.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener

            binding.send.isEnabled = false
            lifecycleScope.launch {
                runCatching { repo.sendMessage(chatId, myUid, text) }
                    .onSuccess {
                        binding.input.setText("")
                        binding.send.isEnabled = false
                        binding.messages.scrollToPosition(adapter.itemCount - 1)
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

private const val VIEW_TYPE_SENT = 1
private const val VIEW_TYPE_RECEIVED = 2

/**
 * RecyclerView adapter that renders a mixed list of messages:
 * - Sent by me → uses `item_message_sent.xml`
 * - Received   → uses `item_message_received.xml`
 *
 * For simplicity this uses `notifyDataSetChanged()`; consider DiffUtil for large lists.
 */
private class MessagesAdapter(private val myUid: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Message>()

    /** Replaces the entire dataset. For production, switch to DiffUtil for efficiency. */
    fun submit(newItems: List<Message>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    /** Choose view type based on whether I sent the message. */
    override fun getItemViewType(position: Int): Int {
        return if (items[position].senderUid == myUid) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

    /** Inflate the appropriate row layout for the view type. */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_SENT) {
            val binding = ItemMessageSentBinding.inflate(inflater, parent, false)
            SentMessageVH(binding)
        } else {
            val binding = ItemMessageReceivedBinding.inflate(inflater, parent, false)
            ReceivedMessageVH(binding)
        }
    }

    /** Bind each row with its message model. */
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = items[position]
        if (holder is SentMessageVH) holder.bind(message)
        else if (holder is ReceivedMessageVH) holder.bind(message)
    }

    override fun getItemCount() = items.size
}

/**
 * ViewHolder for messages sent by the current user.
 * Binds text and a human-readable time (e.g., "3:42 PM").
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
 * ViewHolder for messages received from the other participant.
 * Binds text and timestamp similarly to [SentMessageVH].
 */
private class ReceivedMessageVH(private val binding: ItemMessageReceivedBinding) :
    RecyclerView.ViewHolder(binding.root) {

    private val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun bind(m: Message) {
        binding.msgText.text = m.text
        binding.time.text = fmt.format(Date(m.sentAt))
    }
}
