package com.cs250.kratos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class SplashViewModel(private val firebaseAuth: FirebaseAuth) : ViewModel() {

    sealed class NavigationEvent {
        object GoToMainApp : NavigationEvent()
        object GoToSignIn : NavigationEvent()
    }

    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    fun decideNextNavigation() {
        if (firebaseAuth.currentUser == null) {
            _navigationEvent.value = NavigationEvent.GoToSignIn
        } else {
            _navigationEvent.value = NavigationEvent.GoToMainApp
        }
    }
}
