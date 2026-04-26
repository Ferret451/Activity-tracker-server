package com.bsuir.Activity_tracker_server.nutrition

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class NutritionSummaryCalculatorTest {

    @Test
    fun `calculate returns totals and grouped meal types`() {
        val breakfastId = UUID.randomUUID()
        val dinnerId = UUID.randomUUID()

        val summary = NutritionSummaryCalculator.calculate(
            listOf(
                NutritionSummaryItem(breakfastId, "Breakfast", 350.0, 20.0, 10.0, 45.0),
                NutritionSummaryItem(breakfastId, "Breakfast", 150.0, 8.0, 4.0, 20.0),
                NutritionSummaryItem(dinnerId, "Dinner", 700.0, 40.0, 22.0, 80.0),
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
}

