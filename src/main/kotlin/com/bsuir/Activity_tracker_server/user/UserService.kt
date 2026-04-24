package com.bsuir.Activity_tracker_server.user

import com.bsuir.Activity_tracker_server.user.exception.DuplicateUserFieldException
import com.bsuir.Activity_tracker_server.user.exception.UserNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    @Transactional(readOnly = true)
    fun getAll(): List<UserResponse> = userRepository.findAll()
        .map(User::toResponse)

    @Transactional(readOnly = true)
    fun getById(id: UUID): UserResponse = userRepository.findByIdOrNull(id)
        ?.toResponse()
        ?: throw UserNotFoundException(id)

    @Transactional
    fun create(request: CreateUserRequest): UserResponse {
        val rawPassword = requireNotNull(request.password)
        ensureUniqueEmail(request.email)
        ensureUniqueUsername(request.username)

        val user = User(
            email = request.email.trim(),
            username = request.username.trim(),
            firstName = request.firstName.trim(),
            lastName = request.lastName.trim(),
            passwordHash = requireNotNull(passwordEncoder.encode(rawPassword)),
        )

        return userRepository.save(user).toResponse()
    }

    @Transactional
    fun update(id: UUID, request: UpdateUserRequest): UserResponse {
        val user = userRepository.findByIdOrNull(id)
            ?: throw UserNotFoundException(id)

        ensureUniqueEmail(request.email, id)
        ensureUniqueUsername(request.username, id)

        user.email = request.email.trim()
        user.username = request.username.trim()
        user.firstName = request.firstName.trim()
        user.lastName = request.lastName.trim()

        return userRepository.save(user).toResponse()
    }

    @Transactional
    fun delete(id: UUID) {
        if (!userRepository.existsById(id)) {
            throw UserNotFoundException(id)
        }

        userRepository.deleteById(id)
    }

    private fun ensureUniqueEmail(email: String, id: UUID? = null) {
        val normalizedEmail = email.trim()
        val exists = if (id == null) {
            userRepository.existsByEmailIgnoreCase(normalizedEmail)
        } else {
            userRepository.existsByEmailIgnoreCaseAndIdNot(normalizedEmail, id)
        }

        if (exists) {
            throw DuplicateUserFieldException("email", normalizedEmail)
        }
    }

    private fun ensureUniqueUsername(username: String, id: UUID? = null) {
        val normalizedUsername = username.trim()
        val exists = if (id == null) {
            userRepository.existsByUsernameIgnoreCase(normalizedUsername)
        } else {
            userRepository.existsByUsernameIgnoreCaseAndIdNot(normalizedUsername, id)
        }

        if (exists) {
            throw DuplicateUserFieldException("username", normalizedUsername)
        }
    }
}
