package com.cs250.kratos

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    private val mockAuth: FirebaseAuth = mock()
    private val mockUser: FirebaseUser = mock()

    @Before
    fun setup() {
        Intents.init()
        // Arrange: Tell the mock auth to return a valid user for this test class
        doReturn(mockUser).`when`(mockAuth).currentUser
        // Use the testing hook in SplashActivity to inject the mock
        SplashActivity.viewModelFactory = TestSplashViewModelFactory(mockAuth)
    }

    @After
    fun teardown() {
        SplashActivity.viewModelFactory = null
        Intents.release()
    }

    @Test
    fun appLaunch_whenLoggedIn_displaysHomeFragment() {
        // Act: Launch the app's true entry point
        ActivityScenario.launch(SplashActivity::class.java)

        // Assert: First, verify we navigated to MainActivity
        intended(hasComponent(MainActivity::class.java.name))

        // Assert: Now that we are on the main screen, check for the fragment's content
        onView(withId(R.id.text_home))
            .check(matches(isDisplayed()))
            .check(matches(withText("This is home Fragment")))
    }
}
