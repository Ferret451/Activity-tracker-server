package com.bsuir.Activity_tracker_server.goal

import kotlin.math.round

object GoalProgressCalculator {
    fun calculateProgressPercent(initial: Double, target: Double, current: Double): Double {
        val delta = target - initial

        if (delta == 0.0) {
            return if (current == target) 100.0 else 0.0
        }

        val raw = ((current - initial) / delta) * 100.0
        return roundToTwo(raw.coerceIn(0.0, 100.0))
    }

    fun isGoalCompleted(initial: Double, target: Double, current: Double): Boolean {
        val delta = target - initial
        return when {
            delta > 0 -> current >= target
            delta < 0 -> current <= target
            else -> current == target
        }
    }

    private fun roundToTwo(value: Double): Double = round(value * 100.0) / 100.0
}

