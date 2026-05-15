package com.bsuir.Activity_tracker_server.bodymetrics

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.io.Serializable
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

data class DailyStepCountId(
    @Column(name = "user_id")
    var userId: UUID = UUID.randomUUID(),

    @Column(name = "step_date")
    var stepDate: LocalDate = LocalDate.MIN,
) : Serializable

@Entity
@IdClass(DailyStepCountId::class)
@Table(name = "daily_step_counts")
class DailyStepCount(
    @Id
    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @Id
    @Column(name = "step_date", nullable = false)
    var stepDate: LocalDate,

    @Column(name = "steps", nullable = false)
    var steps: Int,

    @Column(name = "burned_calories_kcal", nullable = false)
    var burnedCaloriesKcal: Double,

    @Column(name = "walking_duration_minutes", nullable = false)
    var walkingDurationMinutes: Int,

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
