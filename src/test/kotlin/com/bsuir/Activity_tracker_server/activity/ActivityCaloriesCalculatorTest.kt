package com.bsuir.Activity_tracker_server.activity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ActivityCaloriesCalculatorTest {

    @Test
    fun `calculate returns calories with two decimal precision`() {
        val calories = ActivityCaloriesCalculator.calculate(
            weightKg = 72.5,
            met = 8.0,
            durationMinutes = 45,
        )

        assertEquals(435.0, calories)
    }

    @Test
    fun `calculate throws for non-positive duration`() {
        assertThrows(IllegalArgumentException::class.java) {
            ActivityCaloriesCalculator.calculate(
                weightKg = 70.0,
                met = 6.0,
                durationMinutes = 0,
            )
        }
    }
}

