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

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        binding.accountSignIn.setOnClickListener { signIn() }

        // Set listener to launch the RegisterActivity
        binding.createAccountButton.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun getInputEmail() = binding.emailEditText.text.toString().trim()
    private fun getInputPassword() = binding.passwordEditText.text.toString().trim()

    private fun signIn() {
        val email = getInputEmail()
        val password = getInputPassword()

        if (email.isEmpty() || password.isEmpty()) {
            Snackbar.make(binding.root, "Please enter email and password.", Snackbar.LENGTH_LONG).show()
            return
        }

        binding.accountSignIn.isEnabled = false

        lifecycleScope.launch {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                // Success, navigate to main activity
                startActivity(Intent(this@SignInActivity, MainActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                finish()
            } catch (e: Exception) {
                Snackbar.make(binding.root, "Sign in failed: ${e.message}", Snackbar.LENGTH_LONG).show()
                Log.e("SignInActivity", "Sign in failed", e)
                binding.accountSignIn.isEnabled = true
            }
        }
    }
}
