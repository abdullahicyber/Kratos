package com.cs250.kratos.data

import kotlinx.coroutines.delay

/**
 * A fake implementation of AuthRepository for testing purposes.
 * This allows us to control the outcome of repository calls from our tests.
 */
class FakeAuthRepository : AuthRepository() {

    private var shouldSucceed = true
    private var delayMillis = 0L

    /**
     * Configure the fake to throw an exception on the next call.
     */
    fun setShouldFail() {
        shouldSucceed = false
    }

    /**
     * Configure the fake to simulate a network delay.
     */
    fun setDelay(millis: Long) {
        delayMillis = millis
    }

    suspend fun createOrUpdateProfile(displayName: String, photoUrl: String?) {
        delay(delayMillis)
        if (!shouldSucceed) {
            throw Exception("Fake repository error")
        }
        // In a more complex scenario, we could record the fact that this was called.
    }
}
