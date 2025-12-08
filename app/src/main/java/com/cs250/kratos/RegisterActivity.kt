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

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val auth = FirebaseAuth.getInstance()
    private val authRepo = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setTitle("Create Account")

        binding.registerButton.setOnClickListener { registerUser() }
    }

    private fun registerUser() {
        val displayName = binding.displayNameEditText.text.toString().trim()
        val email = binding.emailEditText.text.toString().trim()
        val password = binding.passwordEditText.text.toString().trim()

        if (displayName.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Snackbar.make(binding.root, "Please fill out all fields.", Snackbar.LENGTH_LONG).show()
            return
        }

        binding.registerButton.isEnabled = false

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
                val message = when (e) {
                    is FirebaseAuthWeakPasswordException -> "Your password is too weak."
                    is FirebaseAuthUserCollisionException -> "An account with this email already exists."
                    else -> "Registration failed: ${e.message}"
                }
                Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
                Log.e("RegisterActivity", "User registration failed", e)
                binding.registerButton.isEnabled = true
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
