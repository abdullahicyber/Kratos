package com.cs250.kratos

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.cs250.kratos.data.AuthRepository
import com.cs250.kratos.databinding.ActivityCreateProfileBinding
import com.google.android.material.snackbar.Snackbar

/**
 * CreateProfileViewModelFactory
 *
 * A factory class responsible for creating instances of [CreateProfileViewModel].
 * This is necessary because the ViewModel requires an argument (AuthRepository)
 * in its constructor, which the default ViewModel provider cannot supply automatically.
 */
class CreateProfileViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

/**
 * CreateProfileActivity
 *
 * This activity allows a newly registered user to set up their initial profile,
 * specifically setting their display name. It observes the ViewModel state to
 * handle loading, errors, and successful navigation to the main app.
 */
class CreateProfileActivity : AppCompatActivity() {

    companion object {
        /**
         * This is a testing hook. In a real app, you would use a more robust
         * dependency injection framework like Hilt or Koin to inject the factory.
         * This allows tests to swap in a mock factory.
         */        /**
         * This is a testing hook. In a real app, you would use a more robust
         * dependency injection framework like Hilt or Koin.
         */
        @VisibleForTesting
        var viewModelFactory: ViewModelProvider.Factory? = null
    }

    // ViewBinding instance to access UI elements (edit text, save button)
    private lateinit var binding: ActivityCreateProfileBinding

    // Initialize the ViewModel using the 'viewModels' delegate.
    // If a test factory is provided, use it; otherwise, create a standard factory
    // with a real AuthRepository.
    private val viewModel: CreateProfileViewModel by viewModels {
        viewModelFactory ?: CreateProfileViewModelFactory(AuthRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inflate the layout using ViewBinding
        binding = ActivityCreateProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up the "Save" button listener
        binding.save.setOnClickListener {
            val name = binding.displayName.text.toString()

            // Trigger the save operation in the ViewModel
            viewModel.saveProfile(name)
        }

        // Observe the UI State from the ViewModel
        viewModel.uiState.observe(this) { state ->
            // Prevent double-clicks by only enabling the save button when the app is Idle.
            // If the state is 'Saving', the button becomes disabled.
            binding.save.isEnabled = state is CreateProfileViewModel.ProfileUpdateState.Idle

            when (state) {
                is CreateProfileViewModel.ProfileUpdateState.Success -> {

                    // Success: Navigate to the MainActivity and clear the back stack
                    // so the user cannot go back to the profile creation screen.
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
                is CreateProfileViewModel.ProfileUpdateState.Error -> {
                    // Error: Show a Snackbar with the error message
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                else -> {
                    // Handle Idle and Saving states (e.g., show a progress bar for Saving)
                }
            }
        }
    }
}
