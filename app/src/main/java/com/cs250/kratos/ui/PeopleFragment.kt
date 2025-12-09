package com.cs250.kratos.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.cs250.kratos.ChatActivity
import com.cs250.kratos.SignInActivity
import com.cs250.kratos.data.ChatRepository
import com.cs250.kratos.databinding.FragmentPeopleBinding
import com.cs250.kratos.databinding.ItemUserBinding
import com.cs250.kratos.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class PeopleFragment : Fragment() {

    private var _binding: FragmentPeopleBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val myUid get() = FirebaseAuth.getInstance().currentUser?.uid
    private val chatRepo = ChatRepository()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPeopleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Toast.makeText(requireContext(), "Select a person to start a chat", Toast.LENGTH_SHORT)
            .show()

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(
                Intent(requireContext(), SignInActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            requireActivity().finish()
            return
        }

        binding.recycler.layoutManager = LinearLayoutManager(requireContext())

        db.collection("users").get(Source.SERVER)
            .addOnSuccessListener { snap ->
                bindUsers(snap.toObjects(UserProfile::class.java))
            }
            .addOnFailureListener { serverErr ->
                db.collection("users").get(Source.DEFAULT)
                    .addOnSuccessListener { snap ->
                        Toast.makeText(
                            requireContext(),
                            "Showing cached users (offline / rules issue?)",
                            Toast.LENGTH_SHORT
                        ).show()
                        bindUsers(snap.toObjects(UserProfile::class.java))
                    }
                    .addOnFailureListener { cacheErr ->
                        Toast.makeText(
                            requireContext(),
                            "Failed to load users: ${cacheErr.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
    }

    private fun bindUsers(all: List<UserProfile>) {
        val me = myUid
        val users = all.filter { it.uid.isNotBlank() && it.uid != me }

        if (users.isEmpty()) {
            Toast.makeText(requireContext(), "No other users yet.", Toast.LENGTH_SHORT).show()
        }

        binding.recycler.adapter = UsersAdapter(users, chatRepo, viewLifecycleOwner) { user ->
            startActivity(
                Intent(requireContext(), ChatActivity::class.java)
                    .putExtra(ChatActivity.EXTRA_OTHER_UID, user.uid)
            )
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class UsersAdapter(
        private val items: List<UserProfile>,
        private val chatRepo: ChatRepository,
        private val lifecycleOwner: LifecycleOwner,
        private val onClick: (UserProfile) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.Adapter<UserVH>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserVH {
            val binding =
                ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return UserVH(binding, onClick)
        }

        override fun onBindViewHolder(holder: UserVH, position: Int) = holder.bind(items[position], chatRepo, lifecycleOwner)

        override fun getItemCount() = items.size
    }

    private class UserVH(
        private val binding: ItemUserBinding,
        private val onClick: (UserProfile) -> Unit
    ) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

        fun bind(user: UserProfile, chatRepo: ChatRepository, lifecycleOwner: LifecycleOwner) {
            binding.name.text = user.displayName.ifBlank { user.email }
            binding.chatButton.setOnClickListener { onClick(user) }
            binding.root.setOnClickListener { onClick(user) }

            val myUid = FirebaseAuth.getInstance().currentUser?.uid
            if (myUid != null) {
                chatRepo.getUnreadMessagesCountForUserFlow(myUid, user.uid)
                    .onEach { count ->
                        binding.unreadDot.visibility = if (count > 0) View.VISIBLE else View.GONE
                    }
                    .launchIn(lifecycleOwner.lifecycleScope)
            }
        }
    }
}
