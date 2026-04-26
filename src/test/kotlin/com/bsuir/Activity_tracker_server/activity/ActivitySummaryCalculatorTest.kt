package com.bsuir.Activity_tracker_server.activity

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

class ActivitySummaryCalculatorTest {

    @Test
    fun `calculate returns totals and grouped activity types`() {
        val runTypeId = UUID.randomUUID()
        val walkTypeId = UUID.randomUUID()

        val summary = ActivitySummaryCalculator.calculate(
            listOf(
                ActivitySummaryItem(runTypeId, "Running", 30, 5.4, 420.0),
                ActivitySummaryItem(runTypeId, "Running", 20, 3.1, 270.0),
                ActivitySummaryItem(walkTypeId, "Walking", 45, 3.8, 190.0),
            ),
        )

        assertEquals(95, summary.totalDurationMinutes)
        assertEquals(12.3, summary.totalDistanceKm)
        assertEquals(880.0, summary.totalBurnedCaloriesKcal)
        assertEquals(2, summary.byType.size)
        assertEquals("Running", summary.byType.first().activityTypeName)
        assertEquals(50, summary.byType.first().totalDurationMinutes)
        assertEquals(690.0, summary.byType.first().totalBurnedCaloriesKcal)
    }
}

