package com.cs250.kratos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs250.kratos.data.AuthRepository
import kotlinx.coroutines.launch
/**
 * ViewModel for the Create Profile screen.
 *
 * This class follows the MVVM (Model-View-ViewModel) architecture.
 * It is responsible for:
 * 1. Holding the UI state (is it loading? did it fail?).
 * 2. Handling user interactions (clicking "Save").
 * 3. Communicating with the data layer (AuthRepository) via Coroutines.
 *
 * It survives configuration changes (like screen rotation) so data isn't lost.
 */
class CreateProfileViewModel(private val repository: AuthRepository) : ViewModel() {
    /**
     * A Sealed Class representing the specific states the UI can be in.
     * Using a sealed class forces the UI to handle all possible scenarios
     * (Idle, Loading, Success, Error) in a structured way.
     */
    sealed class ProfileUpdateState {
        object Idle : ProfileUpdateState() // Waiting for user input
        object Saving : ProfileUpdateState()// Network request in progress (show spinner)
        object Success : ProfileUpdateState()// Profile saved successfully (navigate away)
        data class Error(val message: String) : ProfileUpdateState() // Something went wrong
    }
    // Backing property: Mutable internally so we can update it.
    private val _uiState = MutableLiveData<ProfileUpdateState>(ProfileUpdateState.Idle)
    // Exposed property: Immutable LiveData so the Activity/Fragment can observe it
    // but cannot change it directly. This enforces encapsulation.
    val uiState: LiveData<ProfileUpdateState> = _uiState
    /**
     * Triggered when the user clicks the "Save" button.
     *
     * @param displayName The name the user entered in the input field.
     */
    fun saveProfile(displayName: String) {
        // 1. Input Validation: Fail fast if the input is invalid
        if (displayName.isBlank()) {
            _uiState.value = ProfileUpdateState.Error("Please enter a display name")
            return
        }
// 2. Set State to Loading: This triggers the UI to show a progress bar
        _uiState.value = ProfileUpdateState.Saving
// 3. Launch Coroutine: Perform the network operation off the main thread
        // viewModelScope ensures this job is cancelled automatically if the ViewModel is cleared
        viewModelScope.launch {
            // runCatching is a clean way to handle try/catch blocks for Kotlin results
            runCatching {
                repository.createOrUpdateProfile(displayName = displayName, photoUrl = null)
            }.onSuccess {
                // Network call finished successfully
                _uiState.value = ProfileUpdateState.Success
            }.onFailure { e ->
                // Network call threw an exception
                _uiState.value = ProfileUpdateState.Error("Failed to save profile: ${e.message}")
            }
        }
    }
}