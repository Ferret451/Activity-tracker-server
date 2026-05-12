package com.bsuir.Activity_tracker_server.nutrition

import com.bsuir.Activity_tracker_server.user.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.math.BigDecimal

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
            caloriesPer100g = bd("165.00"),
            proteinPer100g = bd("31.00"),
            fatPer100g = bd("3.60"),
            carbsPer100g = bd("0.00"),
        )

        val totals = NutritionTotalsCalculator.calculate(food, bd("150.00"))

        assertEquals(bd("247.50"), totals.caloriesKcal)
        assertEquals(bd("46.50"), totals.proteinG)
        assertEquals(bd("5.40"), totals.fatG)
        assertEquals(bd("0.00"), totals.carbsG)
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
            caloriesPer100g = bd("130.00"),
            proteinPer100g = bd("2.70"),
            fatPer100g = bd("0.30"),
            carbsPer100g = bd("28.00"),
        )

        assertThrows(IllegalArgumentException::class.java) {
            NutritionTotalsCalculator.calculate(food, bd("0.00"))
        }
    }

    private fun bd(value: String): BigDecimal = BigDecimal(value)
}

