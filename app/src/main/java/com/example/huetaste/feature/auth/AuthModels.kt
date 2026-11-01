package com.example.huetaste.feature.auth

import com.example.huetaste.domain.model.AppError

/** Why a sign-in or sign-up request failed, in the terms the login and register screens react to. */
enum class AuthFailure { INVALID_CREDENTIALS, EMAIL_TAKEN, NETWORK, UNKNOWN }

internal fun AppError.toAuthFailure(): AuthFailure = when (code) {
    AppError.INVALID_CREDENTIALS -> AuthFailure.INVALID_CREDENTIALS
    AppError.EMAIL_EXISTS -> AuthFailure.EMAIL_TAKEN
    AppError.NETWORK -> AuthFailure.NETWORK
    else -> AuthFailure.UNKNOWN
}

/** One-off results of the login and register screens. */
sealed interface AuthEvent {
    /** A session (or guest mode) is now stored; open the main screen. */
    data object Authenticated : AuthEvent

    /** A failure that has no spot in the form, such as a network error while registering. */
    data class Failed(val failure: AuthFailure, val message: String?) : AuthEvent
}
