package com.cs250.kratos.ui.home

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeViewModelTest {

    // This rule swaps the background executor used by the Architecture Components with a different one
    // that executes each task synchronously.
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var viewModel: HomeViewModel

    @Before
    fun setup() {
        viewModel = HomeViewModel()
    }

    @Test
    fun `test initial text value`() {
        // Get the value from the LiveData
        val value = viewModel.text.value

        // Assert that the value is what we expect
        assertEquals("This is home Fragment", value)
    }
}
