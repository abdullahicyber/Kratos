package com.cs250.kratos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.cs250.kratos.databinding.ActivityMainBinding
import com.cs250.kratos.ui.MyAccountFragment
import com.cs250.kratos.ui.MyWorkoutsFragment
import com.cs250.kratos.ui.PeopleFragment
/**
 * The entry point for the authenticated area of the application.
 *
 * Responsibilities:
 * 1. Serves as the container for the main application fragments (People, Workouts, Account).
 * 2. Manages the Bottom Navigation Bar logic.
 * 3. Updates the user's FCM (Firebase Cloud Messaging) token to ensure they receive push notifications.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
// --- FCM TOKEN REGISTRATION ---
        // We fetch the current FCM token for this device. This token is required
        // to send push notifications (like new chat alerts) specifically to this phone.
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
// If the user is logged in, save this token to their Firestore profile.
            // This allows the backend (or other clients) to look up "User A" and find "Token XYZ" to send a notification.
            if (uid != null) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(uid)
                    .update("fcmToken", token)
            }
        }
// --- INITIAL FRAGMENT SETUP ---
        // Check if savedInstanceState is null. This indicates the Activity is starting for the FIRST time.
        // If it is NOT null, it means the Activity is being recreated (e.g., after screen rotation),
        // and the FragmentManager automatically restores the previous fragment. We don't want to double-add it.
        if (savedInstanceState == null) {
            supportFragmentManager.commit {// Default to showing the 'PeopleFragment' on launch
                replace(R.id.fragment_container, PeopleFragment())
            }
        }
// --- BOTTOM NAVIGATION LISTENER ---
        // Handle clicks on the bottom tab bar
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_people -> {// Switch to the People (Social) screen
                    supportFragmentManager.commit {
                        replace(R.id.fragment_container, PeopleFragment())
                    }
                    true // Return true to indicate the event was handled
                }
                R.id.navigation_my_workouts -> { // Switch to the Workouts list
                    supportFragmentManager.commit {
                        replace(R.id.fragment_container, MyWorkoutsFragment())
                    }
                    true
                }
                R.id.navigation_my_account -> { // Switch to the User Profile/Account screen
                    supportFragmentManager.commit {
                        replace(R.id.fragment_container, MyAccountFragment())
                    }
                    true
                }
                else -> false
            }
        }
    }
}
