package com.cs250.kratos

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.cs250.kratos.data.AuthRepository
import com.cs250.kratos.databinding.ActivityCreateProfileBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

/**
 * # CreateProfileActivity
 *
 * Screen that lets a newly signed-in user create or update their display profile.
 *
 * Responsibilities:
 * - Collect a display name from the user.
 * - Save or update that profile in Firestore (via [AuthRepository]).
 * - Sync the FirebaseAuth display name with Firestore.
 * - Navigate to [MainActivity] upon success.
 *
 * ## Data Flow
 * 1. User enters their display name and presses “Save”.
 * 2. The name is validated (must be non-empty).
 * 3. [AuthRepository.createOrUpdateProfile] is called inside a coroutine.
 * 4. On success, the user is redirected to [MainActivity].
 * 5. On failure, a Snackbar displays the error and re-enables the save button.
 *
 * ## UI Binding
 * Uses [ActivityCreateProfileBinding] for safe view access and clean layout inflation.
 *
 * ## Layout Requirements
 * The layout file `activity_create_profile.xml` should include:
 * ```xml
 * <layout xmlns:android="http://schemas.android.com/apk/res/android">
 *     <data />
 *     <LinearLayout
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent"
 *         android:orientation="vertical"
 *         android:padding="24dp">
 *
 *         <EditText
 *             android:id="@+id/displayName"
 *             android:hint="Display Name"
 *             android:layout_width="match_parent"
 *             android:layout_height="wrap_content"/>
 *
 *         <Button
 *             android:id="@+id/save"
 *             android:text="Save Profile"
 *             android:layout_width="wrap_content"
 *             android:layout_height="wrap_content"
 *             android:layout_marginTop="16dp"/>
 *     </LinearLayout>
 * </layout>
 * ```
 *
 * ## Best Practices
 * - Always disable the save button during network calls to prevent duplicates.
 * - Use `Snackbar` for user feedback instead of `Toast` for inline context.
 * - Use coroutine `lifecycleScope` for background work tied to activity lifecycle.
 */
class CreateProfileActivity : AppCompatActivity() {

    /** ViewBinding for accessing views defined in `activity_create_profile.xml`. */
    private lateinit var binding: ActivityCreateProfileBinding

    /** Repository responsible for Firebase Auth + Firestore profile sync. */
    private val repo = AuthRepository()

    /**
     * Initializes the screen and sets up event handlers.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Handle save button click.
        binding.save.setOnClickListener {
            val name = binding.displayName.text.toString().trim()

            // Validate: display name cannot be empty.
            if (name.isEmpty()) {
                Snackbar.make(binding.root, "Please enter a display name", Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Disable button to prevent multiple clicks.
            binding.save.isEnabled = false

            // Launch coroutine to create/update Firestore profile.
            lifecycleScope.launch {
                runCatching {
                    // Photo picking not implemented yet — pass null for photoUrl.
                    repo.createOrUpdateProfile(displayName = name, photoUrl = null)
                }.onSuccess {
                    // Navigate to main app screen and clear the back stack.
                    startActivity(
                        android.content.Intent(this@CreateProfileActivity, MainActivity::class.java)
                            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    )
                    finish()
                }.onFailure { e ->
                    // On failure: re-enable save button and show error message.
                    binding.save.isEnabled = true
                    Snackbar.make(binding.root, "Failed to save profile: ${e.message}", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }
}
