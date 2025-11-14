package com.cs250.kratos.ui.notifications

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NotificationsViewModelTest {

    // This rule is required for testing LiveData components.
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: NotificationsViewModel

    @Before
    fun setup() {
        viewModel = NotificationsViewModel()
    }

    @Test
    fun `test initial text value`() {
        // Get the value from the LiveData
        val value = viewModel.text.value

        // Assert that the value is what we expect
        assertEquals("This is notifications Fragment", value)
    }
}
