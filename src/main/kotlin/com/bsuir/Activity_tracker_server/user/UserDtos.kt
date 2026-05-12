package com.bsuir.Activity_tracker_server.user

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

data class CreateUserRequest(
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

    val role: UserRole? = null,
)

data class UpdateUserRequest(
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

    val role: UserRole? = null,
)

data class UserResponse(
    val id: UUID,
    val email: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val role: UserRole,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

fun User.toResponse(): UserResponse = UserResponse(
    id = id,
    email = email,
    username = username,
    firstName = firstName,
    lastName = lastName,
    role = role,
    createdAt = requireNotNull(createdAt),
    updatedAt = requireNotNull(updatedAt),
)

data class UpdateUserRoleRequest(
    val role: UserRole,
)
