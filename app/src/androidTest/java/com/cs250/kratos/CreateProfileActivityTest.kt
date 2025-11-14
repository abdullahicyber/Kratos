package com.cs250.kratos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.cs250.kratos.data.AuthRepository
import com.cs250.kratos.data.FakeAuthRepository
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

// A testing-specific factory that uses our FakeAuthRepository
class TestViewModelFactory(private val repository: AuthRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CreateProfileViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@RunWith(AndroidJUnit4::class)
class CreateProfileActivityTest {

    private lateinit var fakeRepository: FakeAuthRepository

    // This rule automatically launches CreateProfileActivity before each test and sets up intent capturing.
    @get:Rule
    val intentsTestRule = IntentsTestRule(CreateProfileActivity::class.java, true, false)

    @Before
    fun setup() {
        fakeRepository = FakeAuthRepository()
        // Replace the real factory with our test factory before the activity is launched
        CreateProfileActivity.viewModelFactory = TestViewModelFactory(fakeRepository)
    }

    @After
    fun teardown() {
        // Reset the factory after the test
        CreateProfileActivity.viewModelFactory = null
    }

    @Test
    fun emptyDisplayName_showsSnackbarError() {
        intentsTestRule.launchActivity(null) // Launch the activity with the test setup
        onView(withId(R.id.save)).perform(click())
        onView(withText("Please enter a display name")).check(matches(isDisplayed()))
    }

    @Test
    fun validDisplayName_disablesSaveButtonOnClick() {
        // Configure the fake repository to have a delay
        fakeRepository.setDelay(1000) // 1 second delay

        intentsTestRule.launchActivity(null) // Launch the activity with the test setup
        onView(withId(R.id.displayName)).perform(typeText("Ada Lovelace"))
        onView(withId(R.id.save)).perform(click())

        // Now, because of the delay, the button will be in a disabled state when we check
        onView(withId(R.id.save)).check(matches(not(isEnabled())))
    }

    @Test
    fun validDisplayName_navigatesToMainActivityOnSuccess() {
        intentsTestRule.launchActivity(null) // Launch the activity with the test setup
        onView(withId(R.id.displayName)).perform(typeText("Ada Lovelace"))
        onView(withId(R.id.save)).perform(click())

        // Assert that an Intent was sent to launch MainActivity
        intended(hasComponent(MainActivity::class.java.name))
    }
}
