package com.bsuir.Activity_tracker_server.auth

import com.bsuir.Activity_tracker_server.user.UserRepository
import com.bsuir.Activity_tracker_server.user.UserRole
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminBootstrapRunner(
    private val userRepository: UserRepository,
    @Value("\${app.security.bootstrap-admin-emails:}") private val bootstrapAdminEmails: String,
) : ApplicationRunner {

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (bootstrapAdminEmails.isBlank()) return
        val emails =
            bootstrapAdminEmails.split(',')
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
        for (email in emails) {
            val user = userRepository.findByEmailIgnoreCase(email) ?: continue
            if (user.role != UserRole.ADMIN) {
                user.role = UserRole.ADMIN
                userRepository.save(user)
            }
        }
    }
}
