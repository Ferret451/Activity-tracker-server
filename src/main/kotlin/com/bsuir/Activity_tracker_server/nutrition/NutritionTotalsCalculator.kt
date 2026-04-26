package com.bsuir.Activity_tracker_server.nutrition

import kotlin.math.round

data class NutritionTotals(
    val caloriesKcal: Double,
    val proteinG: Double,
    val fatG: Double,
    val carbsG: Double,
)

object NutritionTotalsCalculator {
    fun calculate(foodItem: FoodItem, grams: Double): NutritionTotals {
        require(grams > 0.0) { "Grams must be greater than zero" }

        val factor = grams / 100.0
        return NutritionTotals(
            caloriesKcal = roundToTwo(foodItem.caloriesPer100g * factor),
            proteinG = roundToTwo(foodItem.proteinPer100g * factor),
            fatG = roundToTwo(foodItem.fatPer100g * factor),
            carbsG = roundToTwo(foodItem.carbsPer100g * factor),
        )
    }

    private fun roundToTwo(value: Double): Double = round(value * 100) / 100
}

