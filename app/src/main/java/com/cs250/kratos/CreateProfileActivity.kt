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

class CreateProfileViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class CreateProfileActivity : AppCompatActivity() {

    companion object {
        /**
         * This is a testing hook. In a real app, you would use a more robust
         * dependency injection framework like Hilt or Koin.
         */
        @VisibleForTesting
        var viewModelFactory: ViewModelProvider.Factory? = null
    }

    private lateinit var binding: ActivityCreateProfileBinding

    private val viewModel: CreateProfileViewModel by viewModels {
        viewModelFactory ?: CreateProfileViewModelFactory(AuthRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.save.setOnClickListener {
            val name = binding.displayName.text.toString()
            viewModel.saveProfile(name)
        }

        viewModel.uiState.observe(this) { state ->
            // Button is enabled only when in the Idle state.
            binding.save.isEnabled = state is CreateProfileViewModel.ProfileUpdateState.Idle

            when (state) {
                is CreateProfileViewModel.ProfileUpdateState.Success -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
                    finish()
                }
                is CreateProfileViewModel.ProfileUpdateState.Error -> {
                    Snackbar.make(binding.root, state.message, Snackbar.LENGTH_LONG).show()
                }
                else -> {
                    // Handle Idle and Saving states (e.g., show a progress bar for Saving)
                }
            }
        }
    }
}
