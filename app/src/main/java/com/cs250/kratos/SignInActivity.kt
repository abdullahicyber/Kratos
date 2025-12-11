package com.cs250.kratos

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cs250.kratos.databinding.ActivitySignInBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * SignInActivity
 *
 * This activity handles the user login process using Firebase Authentication.
 * It allows existing users to sign in with email/password and provides a link
 * to the registration screen for new users.
 */
class SignInActivity : AppCompatActivity() {

    // ViewBinding instance to access UI elements (email/password fields, buttons)
    private lateinit var binding: ActivitySignInBinding
    // Firebase Auth instance used to verify credentials against the backend
    private val auth = FirebaseAuth.getInstance()

    /**
     * Called when the activity is starting.
     * Initializes the UI, hides the action bar, and sets up button listeners.
     */
    override fun onCreate(savedInstanceState: Bundle?) {

        // Inflate the layout using ViewBinding
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide the default action bar for a cleaner, full-screen login look
        supportActionBar?.hide()

        // Listener for the "Sign In" button
        binding.accountSignIn.setOnClickListener { signIn() }

        // Set listener to launch the RegisterActivity
        binding.createAccountButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    // Helper functions to retrieve trimmed text from input fields
    private fun getInputEmail() = binding.emailEditText.text.toString().trim()
    private fun getInputPassword() = binding.passwordEditText.text.toString().trim()

    /**
     * signIn
     *
     * Validates input fields and attempts to authenticate the user via Firebase.
     * Uses Coroutines to handle the network operation asynchronously.
     */
    private fun signIn() {
        val email = getInputEmail()
        val password = getInputPassword()

        // 1. Basic Validation: Ensure fields are not empty
        if (email.isEmpty() || password.isEmpty()) {
            Snackbar.make(binding.root, "Please enter email and password.", Snackbar.LENGTH_LONG).show()
            return
        }

        // 2. UI Feedback: Disable button to prevent multiple clicks during network request
        binding.accountSignIn.isEnabled = false

        // 3. Perform Authentication
        // Use lifecycleScope to launch a coroutine that is tied to this Activity's lifecycle
        lifecycleScope.launch {
            try {

                // Attempt to sign in. .await() suspends execution until the task completes
                // without blocking the main UI thread.
                auth.signInWithEmailAndPassword(email, password).await()

                // 4. Success Handling
                // Navigate to MainActivity and clear the back stack so the user can't "back" into the login screen
                startActivity(Intent(this@SignInActivity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            } catch (e: Exception) {

                // 5. Failure Handling
                // Show error message to user, log the error, and re-enable the button
                Snackbar.make(binding.root, "Sign in failed: ${e.message}", Snackbar.LENGTH_LONG).show()
                Log.e("SignInActivity", "Sign in failed", e)
                binding.accountSignIn.isEnabled = true
            }
        }
    }
}
