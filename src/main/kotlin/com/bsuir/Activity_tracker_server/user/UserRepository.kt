package com.bsuir.Activity_tracker_server.user

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserRepository : JpaRepository<User, UUID> {
    fun findByEmailIgnoreCase(email: String): User?
    fun existsByEmailIgnoreCase(email: String): Boolean
    fun existsByUsernameIgnoreCase(username: String): Boolean
    fun existsByEmailIgnoreCaseAndIdNot(email: String, id: UUID): Boolean
    fun existsByUsernameIgnoreCaseAndIdNot(username: String, id: UUID): Boolean
}
