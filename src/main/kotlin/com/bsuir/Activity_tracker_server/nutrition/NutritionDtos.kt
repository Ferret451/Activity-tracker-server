package com.bsuir.Activity_tracker_server.nutrition

import jakarta.validation.Valid
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
    val updatedAt: OffsetDateTime,
)

data class AdditionalNutrientRequest(
    @field:Size(max = 16)
    val number: String? = null,

    @field:NotBlank
    @field:Size(max = 200)
    val name: String,

    @field:Size(max = 32)
    val unitName: String? = null,

    @field:DecimalMin(value = "0.0")
    val amountPer100g: Double,
)

data class AdditionalNutrientPer100gResponse(
    val number: String?,
    val name: String,
    val unitName: String?,
    val amountPer100g: Double,
)

data class AdditionalNutrientPortionResponse(
    val number: String?,
    val name: String,
    val unitName: String?,
    val totalAmount: Double,
)

data class AdditionalNutrientTotalResponse(
    val number: String?,
    val name: String,
    val unitName: String?,
    val totalAmount: Double,
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

    @field:Size(max = 100)
    @field:Valid
    val additionalNutrients: List<AdditionalNutrientRequest>? = null,
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

    @field:Size(max = 100)
    @field:Valid
    val additionalNutrients: List<AdditionalNutrientRequest>? = null,
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
    val additionalNutrientsPer100g: List<AdditionalNutrientPer100gResponse> = emptyList(),
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
    val additionalNutrients: List<AdditionalNutrientPortionResponse> = emptyList(),
)

data class NutritionMealTypeSummaryResponse(
    val mealTypeId: UUID,
    val mealTypeName: String,
    val totalCaloriesKcal: Double,
    val totalProteinG: Double,
    val totalFatG: Double,
    val totalCarbsG: Double,
    val additionalNutrientsTotals: List<AdditionalNutrientTotalResponse> = emptyList(),
)

data class NutritionSummaryResponse(
    val totalCaloriesKcal: Double,
    val totalProteinG: Double,
    val totalFatG: Double,
    val totalCarbsG: Double,
    val byMealType: List<NutritionMealTypeSummaryResponse>,
    val additionalNutrientsTotals: List<AdditionalNutrientTotalResponse> = emptyList(),
)

data class NutritionHistoryDayResponse(
    val date: LocalDate,
    val totalCaloriesKcal: Double,
    val totalProteinG: Double,
    val totalFatG: Double,
    val totalCarbsG: Double,
    val records: List<NutritionRecordResponse>,
    val additionalNutrientsTotals: List<AdditionalNutrientTotalResponse> = emptyList(),
)

data class NutritionQueryFilter(
    val from: LocalDate?,
    val to: LocalDate?,
    val mealTypeId: UUID?,
    val foodItemId: UUID?,
)

enum class NutritionDailyCalorieTargetSource {
    EXPLICIT_DAILY_TARGET,
    BMR_GOAL_ADJUSTED,
}

data class NutritionDailyResponse(
    val date: LocalDate,
    val zone: String,
    val totalCaloriesKcal: Double,
    val totalProteinG: Double,
    val totalFatG: Double,
    val totalCarbsG: Double,
    val targetCaloriesKcal: Double,
    val calorieTargetSource: NutritionDailyCalorieTargetSource,
    val remainingCaloriesKcal: Double,
    val additionalNutrientsTotals: List<AdditionalNutrientTotalResponse> = emptyList(),
)

