package com.bsuir.Activity_tracker_server.nutrition

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
@Table(name = "food_items")
class FoodItem(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "name", nullable = false, length = 120)
    var name: String,

    @Column(name = "source", length = 30)
    var source: String? = null,

    @Column(name = "fdc_id")
    var fdcId: Long? = null,

    @Column(name = "calories_per_100g", nullable = false)
    var caloriesPer100g: Double,

    @Column(name = "protein_per_100g", nullable = false)
    var proteinPer100g: Double,

    @Column(name = "fat_per_100g", nullable = false)
    var fatPer100g: Double,

    @Column(name = "carbs_per_100g", nullable = false)
    var carbsPer100g: Double,

    @Column(name = "note", length = 500)
    var note: String? = null,

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

