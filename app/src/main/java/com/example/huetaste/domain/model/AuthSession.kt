package com.example.huetaste.domain.model

data class UserProfile(
    val id: String,
    val email: String,
    val name: String,
    val avatarUrl: String?,
)

/** A signed-in user's credentials; the repository keeps them so later launches do not ask for a login. */
data class AuthSession(
    val accessToken: String,
    val refreshToken: String,
    val user: UserProfile,
)

/** Who is using the app, which decides whether the launch screen goes to the login or straight to the main screen. */
enum class AuthState { SIGNED_IN, GUEST, SIGNED_OUT }
