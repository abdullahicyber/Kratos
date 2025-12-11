package com.cs250.kratos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cs250.kratos.data.AuthRepository
import com.cs250.kratos.databinding.ActivityRegisterBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
/**
 * Handles the user registration process.
 *
 * This Activity orchestrates a two-step registration flow:
 * 1. Creates a raw user account in Firebase Authentication (email/password).
 * 2. Creates a user profile document in Firestore (display name, photo, etc.).
 *
 * It implements manual "transactional" logic to ensure data consistency:
 * if Step 2 fails, it rolls back Step 1 by deleting the Auth account.
 */
class RegisterActivity : AppCompatActivity() {
    // ViewBinding replaces `findViewById`, creating a direct reference to views in activity_register.xml
    private lateinit var binding: ActivityRegisterBinding
    // Firebase instances
    private val auth = FirebaseAuth.getInstance()
    private val authRepo = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        // Enable the "Back" arrow in the top action bar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle("Create Account")

        binding.registerButton.setOnClickListener { registerUser() }
    }
    /**
     * Attempts to register a new user with the provided credentials.
     * Launches a coroutine on the UI thread to handle asynchronous network operations.
     */
    private fun registerUser() {
        // 1. Input Validation: Gather and trim inputs
        val displayName = binding.displayNameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()
// Fast fail: Ensure no fields are empty before hitting the network
        if (displayName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Snackbar.make(binding.root, "Please fill out all fields.", Snackbar.LENGTH_LONG).show()
            return
        }
// Disable button to prevent double-clicks while loading
        binding.registerButton.isEnabled = false
// Launch a coroutine tied to the Activity's lifecycle
        lifecycleScope.launch {
            try {
                // 1. Create the user in Firebase Auth
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                val user = authResult.user
                if (user == null) {
                    throw IllegalStateException("User was not created successfully.")
                }

                // 2. Create the user profile in Firestore
                try {
                    authRepo.createOrUpdateProfile(displayName = displayName, photoUrl = null)

                    // 3. Navigate to MainActivity on full success
                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                    finish()
                } catch (e: Exception) {
                    // If profile creation fails, delete the auth user to roll back
                    user.delete().await()
                    throw e // Re-throw to be caught by the outer block
                }

            } catch (e: Exception) {
                // --- ERROR HANDLING ---
                // Map specific Firebase exceptions to user-friendly messages
                val message = when (e) {
                    is FirebaseAuthWeakPasswordException -> "Your password is too weak."
                    is FirebaseAuthUserCollisionException -> "An account with this email already exists."
                    else -> "Registration failed: ${e.message}"
                }
                // Show error to user
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                Log.e("RegisterActivity", "User registration failed", e)
                // Re-enable the button so they can try again
                binding.registerButton.isEnabled = true
            }
        }
    }
    /**
     * Handles the "Back" arrow click in the Action Bar.
     */
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
