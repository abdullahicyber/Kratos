package com.cs250.kratos

/**
 * A simple object to validate email format, independent of the Android framework.
 */
object EmailValidator {

    // This is the same regex used by android.util.Patterns.EMAIL_ADDRESS
    private val EMAIL_ADDRESS_PATTERN = Regex(
        "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
        "\\@" +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
        "(" +
        "\\." +
        "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,25}" +
        ")+"
    )

    /**
     * Checks if the given email has a valid format.
     */
    fun isValidEmail(email: CharSequence?): Boolean {
        return !email.isNullOrEmpty() && EMAIL_ADDRESS_PATTERN.matches(email)
    }
}
