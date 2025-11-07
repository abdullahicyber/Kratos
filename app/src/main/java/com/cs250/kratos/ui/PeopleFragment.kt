package com.cs250.kratos.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.cs250.kratos.ChatActivity
import com.cs250.kratos.SignInActivity
import com.cs250.kratos.databinding.FragmentPeopleBinding
import com.cs250.kratos.databinding.ItemUserBinding
import com.cs250.kratos.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

/**
 * # PeopleFragment
 *
 * Displays a scrollable list of all users registered in Firestore
 * (from the `users/` collection). Selecting a user opens a 1-on-1
 * chat with them via [ChatActivity].
 *
 * ## Responsibilities
 * - Guard access: only signed-in users can view the people list.
 * - Fetch user profiles from Firestore (`users` collection).
 * - Fall back to Firestore’s local cache if the server fetch fails.
 * - Display users in a [RecyclerView] using a simple adapter.
 * - Exclude the current user from the list.
 *
 * ## Lifecycle Overview
 * - **onCreateView:** Inflates the layout with ViewBinding.
 * - **onViewCreated:** Checks authentication, fetches Firestore users,
 *   and sets up RecyclerView binding.
 * - **onDestroyView:** Clears ViewBinding to prevent memory leaks.
 *
 * ## Firestore Access
 * This fragment first tries to fetch from the **server** for fresh data.
 * If that fails (e.g., offline or permission issue), it automatically
 * falls back to the **local cache**.
 *
 * ## UI Interaction
 * - Clicking a user row (or its chat button) opens a chat:
 *   ```kotlin
 *   Intent(requireContext(), ChatActivity::class.java)
 *       .putExtra(ChatActivity.EXTRA_OTHER_UID, user.uid)
 *   ```
 *
 * ## Layouts
 * - **fragment_people.xml:** Defines the RecyclerView container.
 * - **item_user.xml:** Defines each user row (includes name & “Chat” button).
 */
class PeopleFragment : Fragment() {

    /** Backing property for ViewBinding. */
    private var _binding: FragmentPeopleBinding? = null

    /** Non-null binding, valid only between onCreateView and onDestroyView. */
    private val binding get() = _binding!!

    /** Firestore reference used to load user profiles. */
    private val db = FirebaseFirestore.getInstance()

    /** Convenience accessor for the currently signed-in user’s UID. */
    private val myUid get() = FirebaseAuth.getInstance().currentUser?.uid

    /**
     * Inflates the fragment layout.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPeopleBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Called once the fragment’s view hierarchy is created.
     * Performs authentication checks and loads the user list.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        Toast.makeText(requireContext(), "Select a person to start a chat", Toast.LENGTH_SHORT).show()

        // 1️⃣ Guard: ensure the user is signed in
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(
                Intent(requireContext(), SignInActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            )
            requireActivity().finish()
            return
        }

        // 2️⃣ Set up the RecyclerView for vertical list layout
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())

        // 3️⃣ Attempt online fetch first (SERVER → DEFAULT)
        db.collection("users").get(Source.SERVER)
            .addOnSuccessListener { snap ->
                android.util.Log.d("PeopleFragment", "Online fetch OK (users=${snap.size()})")
                bindUsers(snap.toObjects(UserProfile::class.java))
            }
            .addOnFailureListener { serverErr ->
                android.util.Log.e("PeopleFragment", "Server fetch failed, trying cache", serverErr)
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
                        android.util.Log.e("PeopleFragment", "Cache fetch failed", cacheErr)
                        Toast.makeText(
                            requireContext(),
                            "Failed to load users: ${cacheErr.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
    }

    /**
     * Binds the list of [UserProfile]s to the RecyclerView.
     * Filters out the current user to avoid self-chatting.
     *
     * @param all The full list of user profiles fetched from Firestore.
     */
    private fun bindUsers(all: List<UserProfile>) {
        val me = myUid
        val users = all.filter { it.uid.isNotBlank() && it.uid != me }

        if (users.isEmpty()) {
            Toast.makeText(requireContext(), "No other users yet.", Toast.LENGTH_SHORT).show()
        }

        binding.recycler.adapter = UsersAdapter(users) { user ->
            // On click → open chat screen
            startActivity(
                Intent(requireContext(), ChatActivity::class.java)
                    .putExtra(ChatActivity.EXTRA_OTHER_UID, user.uid)
            )
        }
    }

    /**
     * Clears the binding reference when the view is destroyed.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

/**
 * # UsersAdapter
 *
 * Simple RecyclerView adapter to display a list of [UserProfile]s.
 *
 * Each row shows a user's display name and provides a “Chat” button.
 * Clicking the button or the entire row triggers the provided [onClick] lambda.
 */
private class UsersAdapter(
    private val items: List<UserProfile>,
    private val onClick: (UserProfile) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<UserVH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserVH {
        val binding = ItemUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserVH(binding, onClick)
    }

    override fun onBindViewHolder(holder: UserVH, position: Int) = holder.bind(items[position])

    override fun getItemCount() = items.size
}

/**
 * # UserVH (ViewHolder)
 *
 * A lightweight holder for a single user row.
 * Handles name display and click listeners for chat actions.
 */
private class UserVH(
    private val binding: ItemUserBinding,
    private val onClick: (UserProfile) -> Unit
) : androidx.recyclerview.widget.RecyclerView.ViewHolder(binding.root) {

    /**
     * Binds the given [UserProfile] to the row’s UI.
     * Displays either the display name or email if no name is set.
     *
     * @param user The user to display.
     */
    fun bind(user: UserProfile) {
        binding.name.text = user.displayName.ifBlank { user.email }
        // Respond to both row click and chat button click
        binding.chatButton.setOnClickListener { onClick(user) }
        binding.root.setOnClickListener { onClick(user) }
    }
}
