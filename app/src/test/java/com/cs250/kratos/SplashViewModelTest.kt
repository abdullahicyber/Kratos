package com.cs250.kratos

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class SplashViewModelTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // 1. Create mocks for the Firebase dependencies
    private val mockAuth: FirebaseAuth = mock()
    private val mockUser: FirebaseUser = mock()

    @Test
    fun `when user is null, navigates to sign in`() {
        // 2. Define the behavior of the mock
        // In this test, we want currentUser to be null
        doReturn(null).`when`(mockAuth).currentUser

        // 3. Create the ViewModel with the mock dependency
        val viewModel = SplashViewModel(mockAuth)

        // 4. Call the function we want to test
        viewModel.decideNextNavigation()

        // 5. Check the result
        val expectedEvent = SplashViewModel.NavigationEvent.GoToSignIn
        assertEquals(expectedEvent, viewModel.navigationEvent.value)
    }

    @Test
    fun `when user exists, navigates to main app`() {
        // 2. Define the behavior of the mock
        // In this test, we want currentUser to return our mockUser
        doReturn(mockUser).`when`(mockAuth).currentUser

        // 3. Create the ViewModel with the mock dependency
        val viewModel = SplashViewModel(mockAuth)

        // 4. Call the function we want to test
        viewModel.decideNextNavigation()

        // 5. Check the result
        val expectedEvent = SplashViewModel.NavigationEvent.GoToMainApp
        assertEquals(expectedEvent, viewModel.navigationEvent.value)
    }
}
