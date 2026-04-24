package com.bsuir.Activity_tracker_server.auth

import com.bsuir.Activity_tracker_server.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    var tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: OffsetDateTime,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime? = null,

    @Column(name = "revoked_at")
    var revokedAt: OffsetDateTime? = null,
) {
    @PrePersist
    fun onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC)
        }
    }

    fun isActive(now: OffsetDateTime): Boolean = revokedAt == null && expiresAt.isAfter(now)
}

