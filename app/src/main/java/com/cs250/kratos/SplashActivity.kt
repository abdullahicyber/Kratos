package com.cs250.kratos

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth
/**
 * A custom Factory to inject dependencies into the SplashViewModel.
 *
 * In Android, ViewModels are usually created by the system, so we can't just pass arguments
 * (like FirebaseAuth) to the constructor directly. We need this Factory to tell the
 * system *how* to build the ViewModel with our specific dependencies.
 */
class SplashViewModelFactory(private val firebaseAuth: FirebaseAuth) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SplashViewModel(firebaseAuth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
/**
 * The initial entry point of the application (Splash Screen).
 *
 * Responsibilities:
 * 1. Shows the user a loading screen (usually the app logo) immediately on launch.
 * 2. Determines the user's authentication state (Logged in vs Logged out).
 * 3. Routes the user to the correct screen (Main App or Sign In) without user interaction.
 *
 * This Activity has no UI (XML) logic because it acts purely as a router.
 */
class SplashActivity : AppCompatActivity() {

    companion object {
        @VisibleForTesting
        var viewModelFactory: ViewModelProvider.Factory? = null
    }

    private val viewModel: SplashViewModel by viewModels {
        viewModelFactory ?: SplashViewModelFactory(FirebaseAuth.getInstance())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
// Note: No setContentView() here. Splash screens usually rely on the Window background
        // defined in the theme (styles.xml) to show the logo instantly, preventing a "white flash".

        // --- OBSERVE NAVIGATION EVENTS ---
        // The ViewModel will calculate where to go and post an event here.
        viewModel.navigationEvent.observe(this) { event ->
            val intent = when (event) {
                // User is already logged in -> Go to Home
                is SplashViewModel.NavigationEvent.GoToMainApp -> Intent(this, MainActivity::class.java)
                // User is not logged in -> Go to Login
                is SplashViewModel.NavigationEvent.GoToSignIn -> Intent(this, SignInActivity::class.java)
            }
            // Start the new activity and clear the back stack.
            // FLAG_ACTIVITY_CLEAR_TASK ensures that if the user hits "Back" from the Main/Login screen,
            // they simply exit the app rather than returning to this Splash screen.
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            // Destroy the SplashActivity so it doesn't use resources in the background.
            finish()
        }
// Trigger the logic to check auth status
        viewModel.decideNextNavigation()
    }
}
