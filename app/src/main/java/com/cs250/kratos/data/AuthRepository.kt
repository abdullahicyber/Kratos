package com.cs250.kratos.data

import android.net.Uri
import com.cs250.kratos.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * # AuthRepository
 *
 * A small repository around Firebase Auth and Firestore that:
 * - Reads the current Firebase user (via [FirebaseAuth]).
 * - Ensures/creates a matching profile document in Firestore at `users/{uid}`.
 * - Keeps the FirebaseAuth user display name/photo in sync with the Firestore profile.
 * - Handles signing out the current user.
 *
 * ## Key Ideas
 * - **Source of truth**: We store a canonical `UserProfile` in Firestore. We also mirror
 *   `displayName` and (optionally) `photoUrl` into the FirebaseAuth user so both surfaces match.
 * - **Coroutines**: Methods that talk to the network are `suspend` and use `.await()` to bridge
 *   Firebase Tasks to coroutines.
 * - **Paths**: All profile documents live under `users/{uid}`.
 *
 * ## Threading
 * - Suspend functions should be called from a coroutine scope (e.g., ViewModel or use case).
 *
 * ## Requirements
 * - Callers should ensure the user is authenticated before invoking methods that rely on
 *   `auth.currentUser` (otherwise they will error with `"No user"`).
 *
 * @property auth FirebaseAuth instance (defaults to [FirebaseAuth.getInstance()])
 * @property db Firestore instance (defaults to [FirebaseFirestore.getInstance()])
 */
class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    /**
     * Returns the current Firebase user's UID (nullable).
     *
     * Exists for backward compatibility with code using `currentUID()`.
     *
     * @return the UID string if a user is signed in, otherwise `null`.
     */
    fun currentUID(): String? = auth.currentUser?.uid

    /**
     * Returns the current Firebase user's UID (nullable).
     *
     * CamelCase alias of [currentUID] for style consistency with Kotlin.
     *
     * @return the UID string if a user is signed in, otherwise `null`.
     */
    fun currentUid(): String? = auth.currentUser?.uid

    /**
     * Checks whether a profile document exists at `users/{uid}` for the current user.
     *
     * This does **not** create anything—it only verifies existence.
     *
     * @return `true` if the Firestore document exists, `false` if it doesn't or if no user is signed in.
     *
     * @throws com.google.firebase.FirebaseException if Firestore read fails.
     */
    suspend fun profileExists(): Boolean {
        // If nobody is signed in, we can short-circuit to false.
        val uid = auth.currentUser?.uid ?: return false

        // Read the document once and return whether it exists.
        val snap = db.collection("users").document(uid).get().await()
        return snap.exists()
    }

    /**
     * Ensures a profile document exists for the current user.
     *
     * Behavior:
     * - If `users/{uid}` already exists, it reads and returns that [UserProfile].
     * - If it does **not** exist, it creates a minimal profile from auth fields and returns it.
     *
     * The created profile uses:
     * - `uid`: `user.uid`
     * - `displayName`: `user.displayName` if present; else `user.email`; else `"User"`
     * - `email`: `user.email` (empty if null)
     * - `photoUrl`: `user.photoUrl` string if present
     *
     * @return the profile persisted in Firestore.
     *
     * @throws IllegalStateException if no user is signed in.
     * @throws com.google.firebase.FirebaseException if Firestore read/write fails.
     */
    suspend fun ensureProfile(): UserProfile {
        val user = auth.currentUser ?: error("No user")
        val ref = db.collection("users").document(user.uid)
        val snap = ref.get().await()

        return if (snap.exists()) {
            // Document is present—deserialize it as a UserProfile.
            snap.toObject(UserProfile::class.java)!!
        } else {
            // Create a minimal profile seeded from FirebaseAuth user fields.
            val profile = UserProfile(
                uid = user.uid,
                displayName = user.displayName ?: (user.email ?: "User"),
                email = user.email ?: "",
                photoUrl = user.photoUrl?.toString()
            )
            ref.set(profile).await()
            profile
        }
    }

    /**
     * Creates or updates the current user's profile using values from a "Create/Update Profile" screen.
     *
     * This method **also updates the FirebaseAuth user** so that
     * `FirebaseAuth.getInstance().currentUser?.displayName` and `photoUrl` stay consistent
     * with Firestore's `users/{uid}` document.
     *
     * Typical usage:
     * ```kotlin
     * viewModelScope.launch {
     *   repository.createOrUpdateProfile(
     *     displayName = "Alice Example",
     *     photoUrl = uriOrNull // can be null if not using photos
     *   )
     * }
     * ```
     *
     * @param displayName The display name to show in the app and in FirebaseAuth.
     * @param photoUrl Optional avatar URI; pass `null` if not using photos.
     *
     * @return the [UserProfile] written to Firestore.
     *
     * @throws IllegalStateException if no user is signed in.
     * @throws com.google.firebase.FirebaseException if FirebaseAuth or Firestore operations fail.
     */
    suspend fun createOrUpdateProfile(displayName: String, photoUrl: Uri? = null): UserProfile {
        val user = auth.currentUser ?: error("No user")

        // First, update the FirebaseAuth user so Auth and Firestore remain in sync.
        // This ensures anything relying on FirebaseAuth (e.g., FirebaseUI, other services)
        // sees the same display name and photo.
        val change = userProfileChangeRequest {
            this.displayName = displayName
            if (photoUrl != null) this.photoUri = photoUrl
        }
        user.updateProfile(change).await()

        // Then, write the canonical profile document in Firestore.
        val profile = UserProfile(
            uid = user.uid,
            displayName = displayName,
            email = user.email ?: "",
            photoUrl = photoUrl?.toString()
        )
        db.collection("users").document(user.uid).set(profile).await()
        return profile
    }

    /**
     * Updates the FCM token for a given user.
     *
     * @param uid The user's ID.
     * @param token The new FCM token.
     */
    suspend fun updateFcmToken(uid: String, token: String) {
        db.collection("users").document(uid).update("fcmToken", token).await()
    }


    /**
     * Signs out the current Firebase user.
     *
     * Note: This is synchronous and does not hit the network. After this call,
     * [currentUser][FirebaseAuth.getCurrentUser] becomes `null`.
     */
    fun signOut() {
        auth.signOut()
    }
}
