package com.bsuir.Activity_tracker_server.activity

import kotlin.math.round

object ActivityCaloriesCalculator {
    fun calculate(weightKg: Double, met: Double, durationMinutes: Int): Double {
        require(weightKg > 0.0) { "Weight must be greater than zero" }
        require(met > 0.0) { "MET must be greater than zero" }
        require(durationMinutes > 0) { "Duration must be greater than zero" }

        val hours = durationMinutes / 60.0
        return roundToTwo(met * weightKg * hours)
    }

    private fun roundToTwo(value: Double): Double = round(value * 100) / 100
}

