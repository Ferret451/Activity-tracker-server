package com.bsuir.Activity_tracker_server.profile

import kotlin.math.round

object BmrCalculator {
    fun calculateBmr(sex: Sex, weightKg: Double, heightCm: Int, age: Int): Double {
        val raw = when (sex) {
            Sex.MALE -> 88.36 + (13.4 * weightKg) + (4.8 * heightCm) - (5.7 * age)
            Sex.FEMALE -> 447.6 + (9.2 * weightKg) + (3.1 * heightCm) - (4.3 * age)
        }

        return roundToTwo(raw)
    }

    fun calculateDailyCalories(bmr: Double, activityLevel: ActivityLevel): Double =
        roundToTwo(bmr * activityLevel.multiplier)

    private fun roundToTwo(value: Double): Double = round(value * 100.0) / 100.0
}

