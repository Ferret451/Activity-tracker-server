package com.bsuir.Activity_tracker_server.nutrition

import com.bsuir.Activity_tracker_server.user.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class NutrientTotalsAggregatorTest {

    @Test
    fun `mergeAdditionalNutrients sums by number and name`() {
        val u = user()
        val meal = MealType(name = "Lunch")
        val food = FoodItem(
            user = u,
            name = "Food",
            caloriesPer100g = bd("100"),
            proteinPer100g = bd("1"),
            fatPer100g = bd("0"),
            carbsPer100g = bd("0"),
        )
        val r1 = NutritionRecord(
            user = u,
            mealType = meal,
            foodItem = food,
            grams = bd("100"),
            totalCaloriesKcal = bd("100"),
            totalProteinG = bd("1"),
            totalFatG = bd("0"),
            totalCarbsG = bd("0"),
            consumedAt = java.time.OffsetDateTime.parse("2026-05-01T12:00:00Z"),
        )
        r1.additionalNutrients.add(
            NutritionRecordNutrient(
                nutritionRecord = r1,
                nutrientNumber = "1079",
                name = "fiber, total dietary",
                unitName = "g",
                totalAmount = bd("2.00"),
            ),
        )
        val r2 = NutritionRecord(
            user = u,
            mealType = meal,
            foodItem = food,
            grams = bd("100"),
            totalCaloriesKcal = bd("100"),
            totalProteinG = bd("1"),
            totalFatG = bd("0"),
            totalCarbsG = bd("0"),
            consumedAt = java.time.OffsetDateTime.parse("2026-05-01T13:00:00Z"),
        )
        r2.additionalNutrients.add(
            NutritionRecordNutrient(
                nutritionRecord = r2,
                nutrientNumber = "1079",
                name = "fiber, total dietary",
                unitName = "g",
                totalAmount = bd("1.50"),
            ),
        )

        val merged = NutrientTotalsAggregator.mergeAdditionalNutrients(
            listOf(r1, r2).flatMap { it.additionalNutrients },
        )

        assertEquals(1, merged.size)
        assertEquals(3.5, merged.single().totalAmount, 0.001)
    }

    private fun user() = User(
        id = UUID.randomUUID(),
        email = "a@b.c",
        username = "u",
        firstName = "A",
        lastName = "B",
        passwordHash = "h",
    )

    private fun bd(s: String) = BigDecimal(s)
}
