package com.cs250.kratos

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.auth.FirebaseAuth

class SplashViewModelFactory(private val firebaseAuth: FirebaseAuth) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SplashViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SplashViewModel(firebaseAuth) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

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

        viewModel.navigationEvent.observe(this) { event ->
            val intent = when (event) {
                is SplashViewModel.NavigationEvent.GoToMainApp -> Intent(this, MainActivity::class.java)
                is SplashViewModel.NavigationEvent.GoToSignIn -> Intent(this, SignInActivity::class.java)
            }
            startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
            finish()
        }

        viewModel.decideNextNavigation()
    }
}
