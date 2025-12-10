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

class ChatActivity : AppCompatActivity() {

    companion object { const val EXTRA_OTHER_UID = "otherUid" }

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

        adapter = MessagesAdapter(myUid)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.send.isEnabled = false
        binding.input.doOnTextChanged { text, _, _, _ ->
            binding.send.isEnabled = !text.isNullOrBlank()
        }

        val otherUid = intent.getStringExtra(EXTRA_OTHER_UID)
        if (otherUid == null) {
            finish()
            return
        }

        if (otherUid == myUid) {
            finish()
            return
        }

        chatId = repo.chatIdFor(myUid, otherUid)
        lifecycleScope.launch {
            try {
                repo.updateLastReadTimestamp(chatId, myUid)
            } catch (e: Exception) {
                Log.w("ChatActivity", "Failed to update last read timestamp", e)
            }
        }

        binding.messages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.messages.adapter = adapter

        repo.messagesFlow(chatId)
            .onEach { list ->
                adapter.submit(list)
                if (list.isNotEmpty()) {
                    binding.messages.scrollToPosition(adapter.itemCount - 1)
                }
            }
            .launchIn(lifecycleScope)

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

private const val VIEW_TYPE_SENT = 1
private const val VIEW_TYPE_RECEIVED = 2

private class MessagesAdapter(private val myUid: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<Message>()

    fun submit(newItems: List<Message>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].senderUid == myUid) VIEW_TYPE_SENT else VIEW_TYPE_RECEIVED
    }

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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = items[position]
        if (holder is SentMessageVH) holder.bind(message)
        else if (holder is ReceivedMessageVH) holder.bind(message)
    }

    override fun getItemCount() = items.size
}

private class SentMessageVH(private val binding: ItemMessageSentBinding) :
    RecyclerView.ViewHolder(binding.root) {

    private val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun bind(m: Message) {
        binding.msgText.text = m.text
        binding.time.text = fmt.format(Date(m.sentAt))
    }
}

private class ReceivedMessageVH(private val binding: ItemMessageReceivedBinding) :
    RecyclerView.ViewHolder(binding.root) {

    private val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun bind(m: Message) {
        binding.msgText.text = m.text
        binding.time.text = fmt.format(Date(m.sentAt))
    }
}
