package com.bsuir.Activity_tracker_server.nutrition

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class CreateMealTypeRequest(
    @field:NotBlank
    @field:Size(max = 80)
    val name: String,
)

data class UpdateMealTypeRequest(
    @field:NotBlank
    @field:Size(max = 80)
    val name: String,
)

data class MealTypeResponse(
    val id: UUID,
    val name: String,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class CreateFoodItemRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val name: String,

    @field:DecimalMin(value = "0.0")
    val caloriesPer100g: Double,

    @field:DecimalMin(value = "0.0")
    val proteinPer100g: Double,

    @field:DecimalMin(value = "0.0")
    val fatPer100g: Double,

    @field:DecimalMin(value = "0.0")
    val carbsPer100g: Double,

    @field:Size(max = 30)
    val source: String? = null,

    val fdcId: Long? = null,

    @field:Size(max = 500)
    val note: String? = null,
)

data class UpdateFoodItemRequest(
    @field:NotBlank
    @field:Size(max = 120)
    val name: String,

    @field:DecimalMin(value = "0.0")
    val caloriesPer100g: Double,

    @field:DecimalMin(value = "0.0")
    val proteinPer100g: Double,

    @field:DecimalMin(value = "0.0")
    val fatPer100g: Double,

    @field:DecimalMin(value = "0.0")
    val carbsPer100g: Double,

    @field:Size(max = 30)
    val source: String? = null,

    val fdcId: Long? = null,

    @field:Size(max = 500)
    val note: String? = null,
)

data class FoodItemResponse(
    val id: UUID,
    val name: String,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val carbsPer100g: Double,
    val source: String?,
    val fdcId: Long?,
    val note: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class CreateNutritionRecordRequest(
    val mealTypeId: UUID,
    val foodItemId: UUID,

    @field:DecimalMin(value = "0.1")
    val grams: Double,

    @field:Size(max = 500)
    val note: String? = null,

    val consumedAt: OffsetDateTime? = null,
)

data class UpdateNutritionRecordRequest(
    val mealTypeId: UUID,
    val foodItemId: UUID,

    @field:DecimalMin(value = "0.1")
    val grams: Double,

    @field:Size(max = 500)
    val note: String? = null,

    val consumedAt: OffsetDateTime,
)

data class NutritionRecordResponse(
    val id: UUID,
    val mealType: MealTypeResponse,
    val foodItem: FoodItemResponse,
    val grams: Double,
    val totalCaloriesKcal: Double,
    val totalProteinG: Double,
    val totalFatG: Double,
    val totalCarbsG: Double,
    val note: String?,
    val consumedAt: OffsetDateTime,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class NutritionMealTypeSummaryResponse(
    val mealTypeId: UUID,
    val mealTypeName: String,
    val totalCaloriesKcal: Double,
    val totalProteinG: Double,
    val totalFatG: Double,
    val totalCarbsG: Double,
)

data class NutritionSummaryResponse(
    val totalCaloriesKcal: Double,
    val totalProteinG: Double,
    val totalFatG: Double,
    val totalCarbsG: Double,
    val byMealType: List<NutritionMealTypeSummaryResponse>,
)

data class NutritionHistoryDayResponse(
    val date: LocalDate,
    val totalCaloriesKcal: Double,
    val totalProteinG: Double,
    val totalFatG: Double,
    val totalCarbsG: Double,
    val records: List<NutritionRecordResponse>,
)

data class NutritionQueryFilter(
    val from: LocalDate?,
    val to: LocalDate?,
    val mealTypeId: UUID?,
    val foodItemId: UUID?,
)

