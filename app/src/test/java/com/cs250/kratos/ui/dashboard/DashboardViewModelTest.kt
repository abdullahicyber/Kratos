package com.cs250.kratos.ui.dashboard

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {

    // This rule is required for testing LiveData components.
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: DashboardViewModel

    @Before
    fun setup() {
        viewModel = DashboardViewModel()
    }

    @Test
    fun `test initial text value`() {
        // Get the value from the LiveData
        val value = viewModel.text.value

        // Assert that the value is what we expect
        assertEquals("This is dashboard Fragment", value)
    }
}
