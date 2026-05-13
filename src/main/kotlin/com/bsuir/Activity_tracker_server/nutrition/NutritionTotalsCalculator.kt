package com.bsuir.Activity_tracker_server.nutrition

import java.math.BigDecimal
import java.math.RoundingMode

data class NutritionTotals(
    val caloriesKcal: BigDecimal,
    val proteinG: BigDecimal,
    val fatG: BigDecimal,
    val carbsG: BigDecimal,
)

data class PortionNutrientTotal(
    val nutrientNumber: String?,
    val name: String,
    val unitName: String?,
    val totalAmount: BigDecimal,
)

data class NutritionPortionTotals(
    val caloriesKcal: BigDecimal,
    val proteinG: BigDecimal,
    val fatG: BigDecimal,
    val carbsG: BigDecimal,
    val additionalNutrients: List<PortionNutrientTotal>,
)

object NutritionTotalsCalculator {
    fun calculate(foodItem: FoodItem, grams: BigDecimal): NutritionTotals =
        calculatePortion(foodItem, grams).let {
            NutritionTotals(
                caloriesKcal = it.caloriesKcal,
                proteinG = it.proteinG,
                fatG = it.fatG,
                carbsG = it.carbsG,
            )
        }

    fun calculatePortion(foodItem: FoodItem, grams: BigDecimal): NutritionPortionTotals {
        require(grams > BigDecimal.ZERO) { "Grams must be greater than zero" }

        val factor = grams.divide(BigDecimal("100"), 6, RoundingMode.HALF_UP)
        val additional = foodItem.additionalNutrients.map { n ->
            PortionNutrientTotal(
                nutrientNumber = n.nutrientNumber,
                name = n.name,
                unitName = n.unitName,
                totalAmount = roundToTwo(n.amountPer100g.multiply(factor)),
            )
        }
        return NutritionPortionTotals(
            caloriesKcal = roundToTwo(foodItem.caloriesPer100g.multiply(factor)),
            proteinG = roundToTwo(foodItem.proteinPer100g.multiply(factor)),
            fatG = roundToTwo(foodItem.fatPer100g.multiply(factor)),
            carbsG = roundToTwo(foodItem.carbsPer100g.multiply(factor)),
            additionalNutrients = additional,
        )
    }

    private fun roundToTwo(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)
}

