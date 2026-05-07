package com.bsuir.Activity_tracker_server.profile

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BmrCalculatorTest {

    @Test
    fun `calculates male bmr and daily calories`() {
        val bmr = BmrCalculator.calculateBmr(
            sex = Sex.MALE,
            weightKg = 80.0,
            heightCm = 180,
            age = 30,
        )

        val dailyCalories = BmrCalculator.calculateDailyCalories(bmr, ActivityLevel.MODERATE)

        assertEquals(1853.36, bmr)
        assertEquals(2872.71, dailyCalories)
    }

    @Test
    fun `calculates female bmr`() {
        val bmr = BmrCalculator.calculateBmr(
            sex = Sex.FEMALE,
            weightKg = 60.0,
            heightCm = 165,
            age = 28,
        )

        assertEquals(1390.7, bmr)
    }
}

