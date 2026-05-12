package com.bsuir.Activity_tracker_server.nutrition

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class NutritionSummaryCalculatorTest {

    @Test
    fun `calculate returns totals and grouped meal types`() {
        val breakfastId = UUID.randomUUID()
        val dinnerId = UUID.randomUUID()

        val summary = NutritionSummaryCalculator.calculate(
            listOf(
                NutritionSummaryItem(breakfastId, "Breakfast", bd("350.00"), bd("20.00"), bd("10.00"), bd("45.00")),
                NutritionSummaryItem(breakfastId, "Breakfast", bd("150.00"), bd("8.00"), bd("4.00"), bd("20.00")),
                NutritionSummaryItem(dinnerId, "Dinner", bd("700.00"), bd("40.00"), bd("22.00"), bd("80.00")),
            ),
        )

        assertEquals(1200.0, summary.totalCaloriesKcal)
        assertEquals(68.0, summary.totalProteinG)
        assertEquals(36.0, summary.totalFatG)
        assertEquals(145.0, summary.totalCarbsG)
        assertEquals(2, summary.byMealType.size)
        assertEquals("Dinner", summary.byMealType.first().mealTypeName)
        assertEquals(700.0, summary.byMealType.first().totalCaloriesKcal)
    }

    private fun bd(value: String): BigDecimal = BigDecimal(value)
}

