package com.bsuir.Activity_tracker_server.auth

import com.bsuir.Activity_tracker_server.user.UserRepository
import com.bsuir.Activity_tracker_server.user.UserRole
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class SecurityUserDetailsService(
    private val userRepository: UserRepository,
) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByEmailIgnoreCase(username.trim())
            ?: throw UsernameNotFoundException("User with email '$username' was not found")

        val scope = if (user.role == UserRole.ADMIN) "SCOPE_ADMIN" else "SCOPE_USER"
        return User.withUsername(user.email)
            .password(user.passwordHash)
            .authorities(listOf(SimpleGrantedAuthority(scope)))
            .build()
    }
}

