package com.example.huetaste.domain.model

enum class PasswordStrength { WEAK, MEDIUM, STRONG }

/** Client-side checks that mirror the backend limits, so obvious mistakes never reach the network. */
object AuthValidator {
    const val MIN_PASSWORD_LENGTH = 8
    const val MAX_PASSWORD_LENGTH = 128
    const val MAX_NAME_LENGTH = 150

    private const val STRONG_PASSWORD_LENGTH = 12
    private val emailPattern = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

    fun isEmailValid(email: String): Boolean = emailPattern.matches(email.trim())

    fun isPasswordValid(password: String): Boolean = password.length in MIN_PASSWORD_LENGTH..MAX_PASSWORD_LENGTH

    fun isNameValid(name: String): Boolean = name.trim().length in 1..MAX_NAME_LENGTH

    fun passwordsMatch(password: String, confirmation: String): Boolean = password == confirmation

    /**
     * Weak below the minimum length or with a single kind of character, strong when it is long and mixes
     * letters, digits and symbols, medium in between.
     */
    fun strengthOf(password: String): PasswordStrength {
        if (password.length < MIN_PASSWORD_LENGTH) return PasswordStrength.WEAK
        val kinds = listOf(
            password.any { it.isLowerCase() },
            password.any { it.isUpperCase() },
            password.any { it.isDigit() },
            password.any { !it.isLetterOrDigit() },
        ).count { it }
        return when {
            kinds <= 1 -> PasswordStrength.WEAK
            kinds >= 3 && password.length >= STRONG_PASSWORD_LENGTH -> PasswordStrength.STRONG
            else -> PasswordStrength.MEDIUM
        }
    }
}
