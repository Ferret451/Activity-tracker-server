package com.bsuir.Activity_tracker_server.goal

import com.bsuir.Activity_tracker_server.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(name = "user_goals")
class UserGoal(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Enumerated(EnumType.STRING)
    @Column(name = "goal_type", nullable = false, length = 40)
    var goalType: GoalType,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: GoalStatus = GoalStatus.ACTIVE,

    @Column(name = "title", nullable = false, length = 120)
    var title: String,

    @Column(name = "initial_value", nullable = false)
    var initialValue: BigDecimal,

    @Column(name = "target_value", nullable = false)
    var targetValue: BigDecimal,

    @Column(name = "current_value", nullable = false)
    var currentValue: BigDecimal,

    @Column(name = "start_date", nullable = false)
    var startDate: LocalDate,

    @Column(name = "target_date")
    var targetDate: LocalDate? = null,

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


