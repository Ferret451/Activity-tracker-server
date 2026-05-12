package com.bsuir.Activity_tracker_server.nutrition

import java.util.UUID
import java.math.BigDecimal
import java.math.RoundingMode

data class NutritionSummaryItem(
    val mealTypeId: UUID,
    val mealTypeName: String,
    val caloriesKcal: BigDecimal,
    val proteinG: BigDecimal,
    val fatG: BigDecimal,
    val carbsG: BigDecimal,
)

object NutritionSummaryCalculator {
    fun calculate(items: List<NutritionSummaryItem>): NutritionSummaryResponse {
        val byMealType = items
            .groupBy { it.mealTypeId to it.mealTypeName }
            .map { (key, grouped) ->
                val calories = grouped.fold(BigDecimal.ZERO) { acc, it -> acc + it.caloriesKcal }
                val protein = grouped.fold(BigDecimal.ZERO) { acc, it -> acc + it.proteinG }
                val fat = grouped.fold(BigDecimal.ZERO) { acc, it -> acc + it.fatG }
                val carbs = grouped.fold(BigDecimal.ZERO) { acc, it -> acc + it.carbsG }
                NutritionMealTypeSummaryResponse(
                    mealTypeId = key.first,
                    mealTypeName = key.second,
                    totalCaloriesKcal = roundToTwo(calories.toDouble()),
                    totalProteinG = roundToTwo(protein.toDouble()),
                    totalFatG = roundToTwo(fat.toDouble()),
                    totalCarbsG = roundToTwo(carbs.toDouble()),
                )
            }
            .sortedWith(
                compareByDescending<NutritionMealTypeSummaryResponse> { it.totalCaloriesKcal }
                    .thenBy { it.mealTypeName },
            )

        val totalCalories = items.fold(BigDecimal.ZERO) { acc, it -> acc + it.caloriesKcal }
        val totalProtein = items.fold(BigDecimal.ZERO) { acc, it -> acc + it.proteinG }
        val totalFat = items.fold(BigDecimal.ZERO) { acc, it -> acc + it.fatG }
        val totalCarbs = items.fold(BigDecimal.ZERO) { acc, it -> acc + it.carbsG }

        return NutritionSummaryResponse(
            totalCaloriesKcal = roundToTwo(totalCalories.toDouble()),
            totalProteinG = roundToTwo(totalProtein.toDouble()),
            totalFatG = roundToTwo(totalFat.toDouble()),
            totalCarbsG = roundToTwo(totalCarbs.toDouble()),
            byMealType = byMealType,
        )
    }

    private fun roundToTwo(value: Double): Double =
        BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).toDouble()
}

