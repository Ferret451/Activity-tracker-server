package com.bsuir.Activity_tracker_server.goal

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
@Table(name = "user_goal_progress")
class UserGoalProgress(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goal_id", nullable = false)
    var goal: UserGoal,

    @Column(name = "current_value", nullable = false)
    var value: Double,

    @Column(name = "note", length = 500)
    var note: String? = null,

    @Column(name = "recorded_at", nullable = false)
    var recordedAt: OffsetDateTime? = null,
) {
    @PrePersist
    fun onCreate() {
        if (recordedAt == null) {
            recordedAt = OffsetDateTime.now(ZoneOffset.UTC)
        }
    }
}

