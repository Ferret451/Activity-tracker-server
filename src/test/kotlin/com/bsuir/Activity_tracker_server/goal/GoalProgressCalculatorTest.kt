package com.bsuir.Activity_tracker_server.goal

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class GoalProgressCalculatorTest {

    @Test
    fun `progress increases for growth goal`() {
        val percent = GoalProgressCalculator.calculateProgressPercent(
            initial = 100.0,
            target = 200.0,
            current = 150.0,
        )

        assertEquals(50.0, percent)
        assertFalse(GoalProgressCalculator.isGoalCompleted(100.0, 200.0, 150.0))
        assertTrue(GoalProgressCalculator.isGoalCompleted(100.0, 200.0, 200.0))
    }

    @Test
    fun `progress increases for reduction goal`() {
        val percent = GoalProgressCalculator.calculateProgressPercent(
            initial = 90.0,
            target = 75.0,
            current = 82.5,
        )

        assertEquals(50.0, percent)
        assertFalse(GoalProgressCalculator.isGoalCompleted(90.0, 75.0, 80.0))
        assertTrue(GoalProgressCalculator.isGoalCompleted(90.0, 75.0, 75.0))
    }
}

