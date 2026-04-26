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
@Table(name = "nutrition_records")
class NutritionRecord(
    @Id
    val id: UUID = UUID.randomUUID(),

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meal_type_id", nullable = false)
    var mealType: MealType,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "food_item_id", nullable = false)
    var foodItem: FoodItem,

    @Column(name = "grams", nullable = false)
    var grams: Double,

    @Column(name = "total_calories_kcal", nullable = false)
    var totalCaloriesKcal: Double,

    @Column(name = "total_protein_g", nullable = false)
    var totalProteinG: Double,

    @Column(name = "total_fat_g", nullable = false)
    var totalFatG: Double,

    @Column(name = "total_carbs_g", nullable = false)
    var totalCarbsG: Double,

    @Column(name = "note", length = 500)
    var note: String? = null,

    @Column(name = "consumed_at", nullable = false)
    var consumedAt: OffsetDateTime,

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

