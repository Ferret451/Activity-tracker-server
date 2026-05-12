package com.bsuir.Activity_tracker_server.nutrition

import java.math.BigDecimal
import java.math.RoundingMode

data class NutritionTotals(
    val caloriesKcal: BigDecimal,
    val proteinG: BigDecimal,
    val fatG: BigDecimal,
    val carbsG: BigDecimal,
)

object NutritionTotalsCalculator {
    fun calculate(foodItem: FoodItem, grams: BigDecimal): NutritionTotals {
        require(grams > BigDecimal.ZERO) { "Grams must be greater than zero" }

        val factor = grams.divide(BigDecimal("100"), 6, RoundingMode.HALF_UP)
        return NutritionTotals(
            caloriesKcal = roundToTwo(foodItem.caloriesPer100g.multiply(factor)),
            proteinG = roundToTwo(foodItem.proteinPer100g.multiply(factor)),
            fatG = roundToTwo(foodItem.fatPer100g.multiply(factor)),
            carbsG = roundToTwo(foodItem.carbsPer100g.multiply(factor)),
        )
    }

    private fun roundToTwo(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)
}

