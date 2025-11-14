package com.cs250.kratos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cs250.kratos.data.AuthRepository
import kotlinx.coroutines.launch

class CreateProfileViewModel(private val repository: AuthRepository) : ViewModel() {

    sealed class ProfileUpdateState {
        object Idle : ProfileUpdateState()
        object Saving : ProfileUpdateState()
        object Success : ProfileUpdateState()
        data class Error(val message: String) : ProfileUpdateState()
    }

    private val _uiState = MutableLiveData<ProfileUpdateState>(ProfileUpdateState.Idle)
    val uiState: LiveData<ProfileUpdateState> = _uiState

    fun saveProfile(displayName: String) {
        if (displayName.isBlank()) {
            _uiState.value = ProfileUpdateState.Error("Please enter a display name")
            return
        }

        _uiState.value = ProfileUpdateState.Saving

        viewModelScope.launch {
            runCatching {
                repository.createOrUpdateProfile(displayName = displayName, photoUrl = null)
            }.onSuccess {
                _uiState.value = ProfileUpdateState.Success
            }.onFailure { e ->
                _uiState.value = ProfileUpdateState.Error("Failed to save profile: ${e.message}")
            }
        }
    }
}