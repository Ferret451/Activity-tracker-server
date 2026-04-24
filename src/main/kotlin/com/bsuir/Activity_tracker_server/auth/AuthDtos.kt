package com.bsuir.Activity_tracker_server.auth

import com.bsuir.Activity_tracker_server.user.UserResponse
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

data class RegisterRequest(
    @field:Email
    @field:NotBlank
    @field:Size(max = 255)
    val email: String,

    @field:NotBlank
    @field:Size(max = 100)
    val username: String,

    @field:NotBlank
    @field:Size(max = 100)
    val firstName: String,

    @field:NotBlank
    @field:Size(max = 100)
    val lastName: String,

    @field:NotBlank
    @field:Size(min = 8, max = 100)
    val password: String,
)

data class LoginRequest(
    @field:Email
    @field:NotBlank
    @field:Size(max = 255)
    val email: String,

    @field:NotBlank
    @field:Size(min = 8, max = 100)
    val password: String,
)

data class RefreshTokenRequest(
    @field:NotBlank
    val refreshToken: String,
)

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String,
    val accessTokenExpiresAt: OffsetDateTime,
    val refreshTokenExpiresAt: OffsetDateTime,
    val user: UserResponse,
)

