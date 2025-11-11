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

    // 1) Providers for FirebaseUI (email + google if you want)
    private val providers = arrayListOf(
        AuthUI.IdpConfig.EmailBuilder().build()
        // , AuthUI.IdpConfig.GoogleBuilder().build()
    )

    // 2) Register the FirebaseUI result launcher
    private val launcher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res -> handleResult(res) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.hide()

        // 3) Both "Sign in" and "Create Account" open the same FirebaseUI flow
        val startFlow = {
            val intent = AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setTheme(R.style.Theme_Kratos)             // your orange theme
                .setLogo(R.drawable.ic_fire_emoji)          // your logo drawable
                .setIsSmartLockEnabled(false)               // keep off in dev to avoid confusion
                .enableAnonymousUsersAutoUpgrade()          // optional; remove if not using anon
                .build()
            launcher.launch(intent)
        }

        binding.accountSignIn.setOnClickListener { startFlow() }
        binding.createAccountButton.setOnClickListener { startFlow() }
    }

    override fun onStart() {
        super.onStart()
        // 4) If already signed in -> go straight to the app
        auth.currentUser?.let { goHome() }
    }

    private fun handleResult(res: FirebaseAuthUIAuthenticationResult) {
        val response = res.idpResponse
        if (res.resultCode == RESULT_OK) {
            // (Optional) If your app requires email verification, enforce it here
            val user = auth.currentUser
            if (user?.isEmailVerified == false) {
                toast("Please verify your email before signing in.")
                // user.sendEmailVerification() // uncomment to resend verification if desired
                return
            }
            goHome()
        } else {
            // Log error code to understand why the second login might fail
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
