package com.bsuir.Activity_tracker_server.nutrition

import java.util.UUID
import kotlin.math.round

data class NutritionSummaryItem(
    val mealTypeId: UUID,
    val mealTypeName: String,
    val caloriesKcal: Double,
    val proteinG: Double,
    val fatG: Double,
    val carbsG: Double,
)

object NutritionSummaryCalculator {
    fun calculate(items: List<NutritionSummaryItem>): NutritionSummaryResponse {
        val byMealType = items
            .groupBy { it.mealTypeId to it.mealTypeName }
            .map { (key, grouped) ->
                NutritionMealTypeSummaryResponse(
                    mealTypeId = key.first,
                    mealTypeName = key.second,
                    totalCaloriesKcal = roundToTwo(grouped.sumOf { it.caloriesKcal }),
                    totalProteinG = roundToTwo(grouped.sumOf { it.proteinG }),
                    totalFatG = roundToTwo(grouped.sumOf { it.fatG }),
                    totalCarbsG = roundToTwo(grouped.sumOf { it.carbsG }),
                )
            }
            .sortedWith(
                compareByDescending<NutritionMealTypeSummaryResponse> { it.totalCaloriesKcal }
                    .thenBy { it.mealTypeName },
            )

        return NutritionSummaryResponse(
            totalCaloriesKcal = roundToTwo(items.sumOf { it.caloriesKcal }),
            totalProteinG = roundToTwo(items.sumOf { it.proteinG }),
            totalFatG = roundToTwo(items.sumOf { it.fatG }),
            totalCarbsG = roundToTwo(items.sumOf { it.carbsG }),
            byMealType = byMealType,
        )
    }

    private fun roundToTwo(value: Double): Double = round(value * 100) / 100
}

