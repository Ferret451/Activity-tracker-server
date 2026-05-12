package com.bsuir.Activity_tracker_server.common

import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import java.util.UUID

@Component("accessPolicy")
class AccessPolicy {

    fun isSelfOrAdmin(userId: UUID, authentication: Authentication): Boolean {
        if (isAdmin(authentication)) {
            return true
        }
        val jwt = authentication.principal as? Jwt ?: return false
        return UUID.fromString(jwt.subject) == userId
    }

    fun isAdmin(authentication: Authentication): Boolean =
        authentication.authorities.contains(SimpleGrantedAuthority("SCOPE_ADMIN"))

    fun requireSelfOrAdmin(authentication: Authentication, targetUserId: UUID) {
        if (!isSelfOrAdmin(targetUserId, authentication)) {
            throw org.springframework.security.access.AccessDeniedException("Forbidden")
        }
    }
}
