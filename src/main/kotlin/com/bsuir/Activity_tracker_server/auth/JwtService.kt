package com.bsuir.Activity_tracker_server.auth

import com.bsuir.Activity_tracker_server.user.User
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.OffsetDateTime

@Service
class JwtService(
    private val jwtEncoder: JwtEncoder,
) {
    fun generateAccessToken(
        user: User,
        issuedAt: OffsetDateTime,
        expiresAt: OffsetDateTime,
    ): String {
        val claims = JwtClaimsSet.builder()
            .issuer("activity-tracker-server")
            .subject(user.id.toString())
            .issuedAt(issuedAt.toInstant())
            .expiresAt(expiresAt.toInstant())
            .claim("email", user.email)
            .claim("username", user.username)
            .claim("scope", "USER")
            .build()

        val jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build()
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).tokenValue
    }
}

