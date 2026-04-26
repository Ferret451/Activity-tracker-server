package com.bsuir.Activity_tracker_server.activity

import com.bsuir.Activity_tracker_server.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(name = "activity_records")
class ActivityRecord(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "activity_type_id", nullable = false)
    var activityType: ActivityType,

    @Column(name = "duration_minutes", nullable = false)
    var durationMinutes: Int,

    @Column(name = "distance_km")
    var distanceKm: Double? = null,

    @Column(name = "note", length = 500)
    var note: String? = null,

    @Column(name = "occurred_at", nullable = false)
    var occurredAt: OffsetDateTime,

    @Column(name = "burned_calories_kcal", nullable = false)
    var burnedCaloriesKcal: Double,

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

