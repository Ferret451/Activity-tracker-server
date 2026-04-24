package com.bsuir.Activity_tracker_server.auth

import com.bsuir.Activity_tracker_server.auth.exception.InvalidCredentialsException
import com.bsuir.Activity_tracker_server.auth.exception.InvalidRefreshTokenException
import com.bsuir.Activity_tracker_server.user.User
import com.bsuir.Activity_tracker_server.user.UserRepository
import com.bsuir.Activity_tracker_server.user.toResponse
import com.bsuir.Activity_tracker_server.user.exception.DuplicateUserFieldException
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Duration
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationManager: AuthenticationManager,
    private val jwtService: JwtService,
    @Value("\${app.security.jwt.access-token-ttl:PT15M}") private val accessTokenTtl: Duration,
    @Value("\${app.security.jwt.refresh-token-ttl:P7D}") private val refreshTokenTtl: Duration,
) {
    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        val email = request.email.trim()
        val username = request.username.trim()
        val rawPassword = requireNotNull(request.password)

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw DuplicateUserFieldException("email", email)
        }
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw DuplicateUserFieldException("username", username)
        }

        val user = userRepository.save(
            User(
                email = email,
                username = username,
                firstName = request.firstName.trim(),
                lastName = request.lastName.trim(),
                passwordHash = passwordEncoder.encode(rawPassword)!!,
            ),
        )

        return issueTokens(user)
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val email = request.email.trim()

        try {
            authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(email, request.password),
            )
        } catch (_: BadCredentialsException) {
            throw InvalidCredentialsException()
        }

        val user = userRepository.findByEmailIgnoreCase(email)
            ?: throw InvalidCredentialsException()

        return issueTokens(user)
    }

    @Transactional
    fun refresh(request: RefreshTokenRequest): AuthResponse {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val tokenHash = hashToken(request.refreshToken)

        val refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
            ?: throw InvalidRefreshTokenException()

        if (!refreshToken.isActive(now)) {
            throw InvalidRefreshTokenException()
        }

        refreshToken.revokedAt = now
        refreshTokenRepository.save(refreshToken)

        return issueTokens(refreshToken.user)
    }

    @Transactional
    fun logout(request: RefreshTokenRequest) {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val tokenHash = hashToken(request.refreshToken)
        val token = refreshTokenRepository.findByTokenHash(tokenHash) ?: return

        if (token.revokedAt == null) {
            token.revokedAt = now
            refreshTokenRepository.save(token)
        }
    }

    private fun issueTokens(user: User): AuthResponse {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val accessExpiresAt = now.plus(accessTokenTtl)
        val refreshExpiresAt = now.plus(refreshTokenTtl)
        val rawRefreshToken = generateRefreshToken()

        refreshTokenRepository.save(
            RefreshToken(
                user = user,
                tokenHash = hashToken(rawRefreshToken),
                expiresAt = refreshExpiresAt,
            ),
        )

        return AuthResponse(
            accessToken = jwtService.generateAccessToken(user, now, accessExpiresAt),
            refreshToken = rawRefreshToken,
            tokenType = "Bearer",
            accessTokenExpiresAt = accessExpiresAt,
            refreshTokenExpiresAt = refreshExpiresAt,
            user = user.toResponse(),
        )
    }

    private fun generateRefreshToken(): String = "${UUID.randomUUID()}${UUID.randomUUID()}"

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(token.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString(separator = "") { "%02x".format(it) }
    }
}


