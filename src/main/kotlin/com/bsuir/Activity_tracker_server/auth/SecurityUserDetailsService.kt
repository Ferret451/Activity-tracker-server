package com.bsuir.Activity_tracker_server.auth

import com.bsuir.Activity_tracker_server.user.UserRepository
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

        return User.withUsername(user.email)
            .password(user.passwordHash)
            .authorities("ROLE_USER")
            .build()
    }
}

