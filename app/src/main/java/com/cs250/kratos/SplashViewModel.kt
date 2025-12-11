package com.cs250.kratos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
/**
 * ViewModel responsible for the business logic of the Splash Screen.
 *
 * This class determines the initial state of the application:
 * - If a user is already authenticated -> Navigate to the Main App.
 * - If no user is found -> Navigate to the Sign In screen.
 *
 * It is decoupled from the Android Context, making it easy to unit test
 * by mocking the FirebaseAuth dependency.
 */
class SplashViewModel(private val firebaseAuth: FirebaseAuth) : ViewModel() {
    /**
     * A sealed class defining the specific navigation actions this ViewModel can trigger.
     * Using a sealed class ensures type safety—the Activity only needs to handle
     * these specific events and nothing else.
     */
    sealed class NavigationEvent {
        object GoToMainApp : NavigationEvent() // User is logged in
        object GoToSignIn : NavigationEvent() // User needs to log in
    }
    // Backing property: Mutable allows this class to update the value.
    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    // Exposed property: Immutable LiveData allows the Activity to observe
    // but not modify the event. This protects the integrity of the state.
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent
    /**
     * Checks the current authentication status and updates the navigation event.
     * This is typically called immediately when the SplashActivity is created.
     */
    fun decideNextNavigation() {
        // firebaseAuth.currentUser is null if the user is not logged in or the session expired.
        if (firebaseAuth.currentUser == null) {
            _navigationEvent.value = NavigationEvent.GoToSignIn
        } else {
            _navigationEvent.value = NavigationEvent.GoToMainApp
        }
    }
}
