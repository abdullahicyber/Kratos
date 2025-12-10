package com.cs250.kratos.model

/**
 * # UserProfile
 *
 * Represents a user’s profile document stored in Firestore under:
 * ```
 * users/{uid}
 * ```
 *
 * This is the canonical source of truth for user data in the app.
 * It mirrors basic identity information from Firebase Authentication
 * (like [displayName], [email], and [photoUrl]) and adds your own fields,
 * such as [createdAt].
 *
 * ## Firestore Mapping
 * A typical document looks like:
 * ```
 * uid: "abc123",
 * displayName: "Alice Example",
 * email: "alice@example.com",
 * photoUrl: "https://example.com/alice.jpg",
 * fcmToken: "c123...",
 * createdAt: 1717000000000
 * ```
 *
 * This class is automatically serialized/deserialized by Firestore’s SDK,
 * so you can directly call:
 * ```kotlin
 * val profile = snapshot.toObject(UserProfile::class.java)
 * ```
 *
 * ## Notes
 * - Default values ensure the model never fails to deserialize, even if fields are missing.
 * - [createdAt] defaults to the system time when the profile object is created in code.
 *   Firestore documents can override it using server timestamps if you choose later.
 *
 * ## Typical Use
 * ```kotlin
 * val profile = UserProfile(
 *     uid = "abc123",
 *     displayName = "Alice Example",
 *     email = "alice@example.com",
 *     photoUrl = "https://example.com/alice.jpg"
 * )
 * db.collection("users").document(profile.uid).set(profile)
 * ```
 *
 * @property uid Unique Firebase Authentication user ID. Matches `FirebaseUser.uid`.
 * @property displayName The user's display name shown throughout the app.
 * @property email The user’s email address. May be empty for anonymous users.
 * @property photoUrl Optional URL of the user’s avatar or profile picture.
 * @property fcmToken The user's current Firebase Cloud Messaging token. Null if not available.
 * @property createdAt Epoch timestamp (milliseconds) when the profile was created locally.
 */
data class UserProfile(
        val uid: String = "",
        val displayName: String = "",
        val email: String = "",
        val photoUrl: String? = null,
        val fcmToken: String? = null,
        val createdAt: Long = System.currentTimeMillis()
)
