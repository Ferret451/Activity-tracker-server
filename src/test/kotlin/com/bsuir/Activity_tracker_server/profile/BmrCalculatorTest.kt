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

        val baseDailyCalories = BmrCalculator.calculateBaseDailyCalories(bmr, ActivityLevel.MODERATE)
        val goalAdjustedDailyCalories = BmrCalculator.calculateGoalAdjustedDailyCalories(
            bmr = bmr,
            activityLevel = ActivityLevel.MODERATE,
            calorieGoal = CalorieGoal.MAINTAIN,
        )

        assertEquals(1780.0, bmr)
        assertEquals(2759.0, baseDailyCalories)
        assertEquals(2759.0, goalAdjustedDailyCalories)
    }

    @Test
    fun `calculates female bmr`() {
        val bmr = BmrCalculator.calculateBmr(
            sex = Sex.FEMALE,
            weightKg = 60.0,
            heightCm = 165,
            age = 28,
        )

        assertEquals(1330.25, bmr)
    }

    @Test
    fun `applies calorie goal delta to daily calories`() {
        val bmr = BmrCalculator.calculateBmr(
            sex = Sex.MALE,
            weightKg = 80.0,
            heightCm = 180,
            age = 30,
        )

        val base = BmrCalculator.calculateBaseDailyCalories(bmr, ActivityLevel.SEDENTARY)
        val loss = BmrCalculator.calculateGoalAdjustedDailyCalories(bmr, ActivityLevel.SEDENTARY, CalorieGoal.WEIGHT_LOSS)
        val gain = BmrCalculator.calculateGoalAdjustedDailyCalories(bmr, ActivityLevel.SEDENTARY, CalorieGoal.FAST_WEIGHT_GAIN)

        assertEquals(2136.0, base)
        assertEquals(1536.0, loss)
        assertEquals(2736.0, gain)
    }
}

