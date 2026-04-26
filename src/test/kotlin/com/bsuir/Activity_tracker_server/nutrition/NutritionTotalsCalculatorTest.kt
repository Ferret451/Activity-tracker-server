package com.bsuir.Activity_tracker_server.nutrition

import com.bsuir.Activity_tracker_server.user.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class NutritionTotalsCalculatorTest {

    @Test
    fun `calculate returns nutrient totals for grams`() {
        val food = FoodItem(
            user = User(
                email = "test@example.com",
                username = "user",
                firstName = "Test",
                lastName = "User",
                passwordHash = "hash",
            ),
            name = "Chicken breast",
            caloriesPer100g = 165.0,
            proteinPer100g = 31.0,
            fatPer100g = 3.6,
            carbsPer100g = 0.0,
        )

        val totals = NutritionTotalsCalculator.calculate(food, 150.0)

        assertEquals(247.5, totals.caloriesKcal)
        assertEquals(46.5, totals.proteinG)
        assertEquals(5.4, totals.fatG)
        assertEquals(0.0, totals.carbsG)
    }

    @Test
    fun `calculate throws for non-positive grams`() {
        val food = FoodItem(
            user = User(
                email = "test@example.com",
                username = "user",
                firstName = "Test",
                lastName = "User",
                passwordHash = "hash",
            ),
            name = "Rice",
            caloriesPer100g = 130.0,
            proteinPer100g = 2.7,
            fatPer100g = 0.3,
            carbsPer100g = 28.0,
        )

        assertThrows(IllegalArgumentException::class.java) {
            NutritionTotalsCalculator.calculate(food, 0.0)
        }
    }
}

