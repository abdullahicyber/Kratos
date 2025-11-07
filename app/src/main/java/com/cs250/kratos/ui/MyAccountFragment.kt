package com.cs250.kratos.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.cs250.kratos.SignInActivity
import com.cs250.kratos.data.AuthRepository
import com.cs250.kratos.databinding.FragmentMyAccountBinding
import com.google.firebase.auth.FirebaseAuth

/**
 * # MyAccountFragment
 *
 * A fragment that displays the current user's account information and allows
 * them to **sign out** of the app.
 *
 * This fragment interacts with:
 * - [FirebaseAuth] to read the current authenticated user
 * - [AuthRepository] to handle the sign-out process and data consistency
 * - [SignInActivity] to redirect users when authentication is required
 *
 * ## Responsibilities
 * - Display the current user’s display name and email.
 * - Provide a sign-out button that logs out the user and redirects to the sign-in screen.
 * - Ensure unauthenticated users are immediately redirected away from this screen.
 *
 * ## Lifecycle Summary
 * - **onCreateView:** Inflates the view binding.
 * - **onViewCreated:** Verifies authentication, populates user info, and sets up the sign-out button.
 * - **onDestroyView:** Clears the binding reference to prevent memory leaks.
 *
 * ## Navigation Behavior
 * When signing out:
 * 1. The current user is signed out of Firebase via [AuthRepository.signOut].
 * 2. The app launches [SignInActivity] with flags to **clear the back stack**.
 * 3. The hosting activity is finished so the user cannot navigate back here.
 *
 * ## Layout Requirements
 * The layout file `fragment_my_account.xml` should contain:
 * ```xml
 * <layout xmlns:android="http://schemas.android.com/apk/res/android">
 *     <data />
 *     <LinearLayout
 *         android:layout_width="match_parent"
 *         android:layout_height="match_parent"
 *         android:orientation="vertical"
 *         android:padding="16dp">
 *
 *         <TextView
 *             android:id="@+id/displayName"
 *             android:textStyle="bold"
 *             android:textSize="18sp"
 *             android:layout_width="match_parent"
 *             android:layout_height="wrap_content"/>
 *
 *         <TextView
 *             android:id="@+id/email"
 *             android:layout_width="match_parent"
 *             android:layout_height="wrap_content"/>
 *
 *         <Button
 *             android:id="@+id/signOut"
 *             android:text="Sign Out"
 *             android:layout_width="wrap_content"
 *             android:layout_height="wrap_content"
 *             android:layout_marginTop="16dp"/>
 *     </LinearLayout>
 * </layout>
 * ```
 */
class MyAccountFragment : Fragment() {

    /** Backing property for ViewBinding. */
    private var _binding: FragmentMyAccountBinding? = null

    /** Non-null binding valid only between onCreateView and onDestroyView. */
    private val binding get() = _binding!!

    /** Reference to the authentication repository for sign-out operations. */
    private val repo = AuthRepository()

    /**
     * Inflates the fragment layout using ViewBinding.
     *
     * @param inflater Used to inflate the XML layout.
     * @param container Optional parent view group.
     * @param savedInstanceState Saved instance state, if any.
     * @return The root view for this fragment.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyAccountBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * Called after the view hierarchy has been created.
     *
     * - Checks if a Firebase user is signed in.
     * - Displays their account info.
     * - Configures the "Sign Out" button.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Step 1: Ensure a user is currently authenticated.
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            // No user logged in → redirect to SignInActivity.
            goToSignIn()
            return
        }

        // Step 2: Populate UI with user info.
        binding.email.text = user.email ?: "Unknown"
        binding.displayName.text = user.displayName ?: "(no name)"

        // Step 3: Handle sign-out button click.
        binding.signOut.setOnClickListener {
            // 1) Sign out via AuthRepository
            repo.signOut()
            // 2) Redirect to SignInActivity (clear back stack)
            goToSignIn()
            // 3) Close current activity so user can’t navigate back
            activity?.finish()
        }
    }

    /**
     * Navigates the user to [SignInActivity], clearing all previous activities
     * from the back stack to ensure a clean login flow.
     */
    private fun goToSignIn() {
        startActivity(
            Intent(requireContext(), SignInActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        )
    }

    /**
     * Clears the binding reference when the view is destroyed to prevent memory leaks.
     */
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
