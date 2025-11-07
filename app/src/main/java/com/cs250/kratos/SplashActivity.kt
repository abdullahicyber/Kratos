package com.cs250.kratos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

/**
 * # SplashActivity
 *
 * The lightweight launcher activity of the app.
 *
 * Its only purpose is to decide whether the user should:
 * - Be directed to the **sign-in flow** ([SignInActivity]) if not authenticated, or
 * - Be taken directly to the **main app screen** ([MainActivity]) if already signed in.
 *
 * ## Responsibilities
 * - Initialize Firebase Firestore logging (for debugging/development).
 * - Check Firebase Authentication state.
 * - Route the user accordingly and immediately finish itself.
 *
 * ## Behavior
 * - If `FirebaseAuth.getInstance().currentUser == null` → user is not signed in → go to [SignInActivity].
 * - Else → user is signed in → go to [MainActivity].
 *
 * This activity never stays visible long enough to render a UI; it simply redirects and finishes.
 *
 * ## Navigation Flags
 * Uses:
 * ```kotlin
 * Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
 * ```
 * to clear any existing task stack and prevent the user from returning to the splash screen via back navigation.
 *
 * ## Developer Note
 * Enabling Firestore logging via:
 * ```kotlin
 * FirebaseFirestore.setLoggingEnabled(true)
 * ```
 * prints all Firestore requests and responses to Logcat — extremely useful for debugging
 * reads, writes, or permission issues during development.
 *
 * ## Example Flow
 * ```mermaid
 * graph LR
 * A[Launch App] --> B{User signed in?}
 * B -- Yes --> C[MainActivity]
 * B -- No --> D[SignInActivity]
 * ```
 */
class SplashActivity : AppCompatActivity() {
    /**
     * Called when the activity is first created.
     * Immediately checks authentication and redirects accordingly.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable verbose Firestore debug logging for development (safe to remove in production)
        com.google.firebase.firestore.FirebaseFirestore.setLoggingEnabled(true)

        // Determine next destination based on sign-in state
        val next = if (FirebaseAuth.getInstance().currentUser == null) {
            Intent(this, SignInActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }

        // Navigate to destination and clear activity back stack
        startActivity(
            next.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )

        // Finish SplashActivity so it’s not on the back stack
        finish()
    }
}
