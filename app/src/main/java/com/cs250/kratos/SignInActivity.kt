package com.cs250.kratos

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.cs250.kratos.databinding.ActivitySignInBinding
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth

class SignInActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignInBinding
    private val auth by lazy { FirebaseAuth.getInstance() }

    // Providers for FirebaseUI, now used only for the "Create Account" flow
    private val providers = arrayListOf(
        AuthUI.IdpConfig.EmailBuilder().build()
    )

    // Register the FirebaseUI result launcher for the "Create Account" flow
    private val launcher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res -> handleResult(res) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // --- Custom Sign-In Logic ---
        binding.accountSignIn.setOnClickListener {
            val email = binding.emailEditText.text.toString().trim()
            val password = binding.passwordEditText.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                toast("Please enter both email and password.")
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, navigate to the main activity.
                        goHome()
                    } else {
                        // If sign in fails, display a message to the user.
                        toast("Authentication failed: ${task.exception?.message}")
                    }
                }
        }

        // --- FirebaseUI Flow for "Create Account" ---
        val startCreateAccountFlow = {
            val intent = AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setTheme(R.style.Theme_Kratos)
                .setLogo(R.drawable.ic_fire_emoji)
                .setIsSmartLockEnabled(false)
                .build()
            launcher.launch(intent)
        }

        binding.createAccountButton.setOnClickListener { startCreateAccountFlow() }
    }

    override fun onStart() {
        super.onStart()
        // If user is already signed in, go straight to the app
        auth.currentUser?.let { goHome() }
    }

    private fun handleResult(res: FirebaseAuthUIAuthenticationResult) {
        val response = res.idpResponse
        if (res.resultCode == RESULT_OK) {
            goHome()
        } else {
            val code = response?.error?.errorCode
            android.util.Log.e("AUTH", "FirebaseUI error code=$code", response?.error)
            toast(
                when (code) {
                    ErrorCodes.NO_NETWORK -> "No network connection."
                    ErrorCodes.UNKNOWN_ERROR -> "Unknown error."
                    else -> response?.error?.localizedMessage ?: "Sign-in was cancelled."
                }
            )
        }
    }

    private fun goHome() {
        startActivity(
            Intent(this, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
        finish()
    }

    private fun toast(s: String) =
        android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_LONG).show()
}
