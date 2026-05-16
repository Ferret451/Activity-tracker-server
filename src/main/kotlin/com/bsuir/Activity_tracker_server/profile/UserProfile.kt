package com.bsuir.Activity_tracker_server.profile

import com.bsuir.Activity_tracker_server.user.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import org.springframework.data.domain.Persistable
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Entity
@Table(name = "user_profiles")
class UserProfile(
    @Id
    @Column(name = "user_id", nullable = false)
    var userId: UUID,

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "weight_kg", nullable = false)
    var weightKg: Double,

    @Column(name = "height_cm", nullable = false)
    var heightCm: Int,

    @Column(name = "age", nullable = false)
    var age: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "sex", nullable = false, length = 10)
    var sex: Sex,

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_level", nullable = false, length = 20)
    var activityLevel: ActivityLevel,

    @Enumerated(EnumType.STRING)
    @Column(name = "calorie_goal", nullable = false, length = 30)
    var calorieGoal: CalorieGoal = CalorieGoal.MAINTAIN,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime? = null,
) : Persistable<UUID> {

    override fun getId(): UUID = userId

    override fun isNew(): Boolean = createdAt == null

    @PrePersist
    fun onCreate() {
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        userId = user.id
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    fun onUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC)
    }
}

