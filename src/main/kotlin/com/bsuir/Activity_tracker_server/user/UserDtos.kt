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
)

data class UserResponse(
    val id: UUID,
    val email: String,
    val username: String,
    val firstName: String,
    val lastName: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

fun User.toResponse(): UserResponse = UserResponse(
    id = id,
    email = email,
    username = username,
    firstName = firstName,
    lastName = lastName,
    createdAt = requireNotNull(createdAt),
    updatedAt = requireNotNull(updatedAt),
)
