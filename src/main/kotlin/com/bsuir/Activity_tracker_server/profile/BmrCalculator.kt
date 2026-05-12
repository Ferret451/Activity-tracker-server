package com.bsuir.Activity_tracker_server.profile

import kotlin.math.round

object BmrCalculator {
    fun calculateBmr(sex: Sex, weightKg: Double, heightCm: Int, age: Int): Double {
        val raw = when (sex) {
            Sex.FEMALE -> (10 * weightKg) + (6.25 * heightCm) - (5 * age) - 161
            Sex.MALE -> (10 * weightKg) + (6.25 * heightCm) - (5 * age) + 5
        }

        return roundToTwo(raw)
    }

    fun calculateBaseDailyCalories(bmr: Double, activityLevel: ActivityLevel): Double =
        roundToTwo(bmr * activityLevel.multiplier)

    fun calculateGoalAdjustedDailyCalories(
        bmr: Double,
        activityLevel: ActivityLevel,
        calorieGoal: CalorieGoal,
    ): Double = roundToTwo((bmr * activityLevel.multiplier) + calorieGoal.deltaKcal)

    private fun roundToTwo(value: Double): Double = round(value * 100.0) / 100.0
}

