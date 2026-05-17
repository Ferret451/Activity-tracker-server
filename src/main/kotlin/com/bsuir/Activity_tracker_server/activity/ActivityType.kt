package com.bsuir.Activity_tracker_server.activity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(name = "activity_types")
class ActivityType(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "name", nullable = false, length = 80)
    var name: String,

    @Column(name = "met", nullable = false)
    var met: Double,

    @Column(name = "supports_distance", nullable = false)
    var supportsDistance: Boolean = false,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null,
) {
    @PrePersist
    fun onCreate() {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC)
    }
}
