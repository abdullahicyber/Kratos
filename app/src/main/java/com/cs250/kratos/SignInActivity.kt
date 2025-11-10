package com.cs250.kratos

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.ProgressBar
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.cs250.kratos.databinding.ActivitySignInBinding
import com.cs250.kratos.data.AuthRepository
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * # SignInActivity
 *
 * The app’s authentication entry point.
 * This activity handles signing in with FirebaseUI Auth, ensures the user’s Firestore
 * profile exists, and navigates to [MainActivity] upon success.
 *
 * ## Responsibilities
 * - Launch the FirebaseUI sign-in flow.
 * - Handle the sign-in result through a registered launcher callback.
 * - Create or verify a corresponding Firestore user profile using [AuthRepository].
 * - Show progress indicators and user feedback (via [AlertDialog] and [Snackbar]).
 *
 * ## Authentication Flow
 * 1. User clicks **"Sign In"**.
 * 2. Launches FirebaseUI Auth with Email provider.
 * 3. Upon success:
 *    - Shows a small loading dialog.
 *    - Calls `authRepo.ensureProfile()` to confirm the user has a Firestore document.
 *    - Navigates to [MainActivity] and clears the back stack.
 * 4. Upon failure:
 *    - Displays an appropriate [Snackbar] message.
 *
 * ## Providers
 * Currently supports:
 * - **Email/Password**
 * You can easily add more (e.g., Google, GitHub, etc.) by extending [providers].
 *
 * ## UI
 * Uses [ActivitySignInBinding] to safely access layout elements.
 * Example layout (`activity_sign_in.xml`):
 * ```xml
 * <layout xmlns:android="http://schemas.android.com/apk/res/android">
 *     <data />
 *     <LinearLayout
 *         android:orientation="vertical"
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent"
 *         android:gravity="center"
 *         android:padding="32dp">
 *
 *         <ImageView
 *             android:src="@drawable/ic_fire_emoji"
 *             android:layout_width="wrap_content"
 *             android:layout_height="wrap_content"/>
 *
 *         <Button
 *             android:id="@+id/accountSignIn"
 *             android:text="Sign In"
 *             android:layout_width="wrap_content"
 *             android:layout_height="wrap_content"
 *             android:layout_marginTop="24dp"/>
 *     </LinearLayout>
 * </layout>
 * ```
 *
 * ## Best Practices
 * - Always disable SmartLock during development (`setIsSmartLockEnabled(false)`).
 * - Handle all error cases gracefully (network issues, cancellations, etc.).
 * - Use `CoroutineScope(Dispatchers.Main)` for UI-safe coroutine work.
 */
class SignInActivity : AppCompatActivity() {

    /** ViewBinding instance for layout access. */
    private lateinit var binding: ActivitySignInBinding

    /** Repository for handling authentication and Firestore profile creation. */
    private val authRepo = AuthRepository()

    /** FirebaseUI provider list (Email authentication for now). */
    private val providers = listOf(AuthUI.IdpConfig.EmailBuilder().build())

    /**
     * Activity Result API launcher for FirebaseUI sign-in.
     *
     * Handles both success and failure outcomes and performs
     * profile setup on successful authentication.
     */
    private val launcher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val response = IdpResponse.fromResultIntent(result.data)

        if (result.resultCode == Activity.RESULT_OK) {
            val dialog = AlertDialog.Builder(this)
                .setCancelable(false)
                .setView(ProgressBar(this))
                .create()
            dialog.show()

            // Ensure Firestore profile exists, then navigate to main screen.
            CoroutineScope(Dispatchers.Main).launch {
                runCatching { authRepo.ensureProfile() } // This function should exist in AuthRepository
                    .onSuccess {
                        dialog.dismiss()

                        // --- THIS IS THE FIX ---
                        // Change the destination from CreateProfileActivity back to MainActivity.
                        startActivity(
                            Intent(this@SignInActivity, MainActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                        )
                        finish() // Close the sign-in screen
                    }
                    .onFailure {
                        dialog.dismiss()
                        Snackbar.make(
                            binding.root,
                            "Profile setup failed: ${it.message}",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
            }
        } else {
            if (response == null) return@registerForActivityResult

            when (response.error?.errorCode) {
                ErrorCodes.NO_NETWORK -> {
                    Snackbar.make(binding.root, "No network connection", Snackbar.LENGTH_LONG).show()
                }
                else -> {
                    Snackbar.make(binding.root, "Sign-in canceled or failed", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Initializes the sign-in screen and sets up button actions.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignInBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        // Sign In
        binding.accountSignIn.setOnClickListener {
            val intent = AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setLogo(R.drawable.ic_fire_emoji) // Make sure this drawable exists
                .setIsSmartLockEnabled(false) // Good for development
                .build()
            launcher.launch(intent)
        }

        // Create Account
        binding.createAccountButton.setOnClickListener {
            val intent = AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .setLogo(R.drawable.ic_fire_emoji)
                .setIsSmartLockEnabled(false)
                .build()
            launcher.launch(intent)
        }
    }
}
