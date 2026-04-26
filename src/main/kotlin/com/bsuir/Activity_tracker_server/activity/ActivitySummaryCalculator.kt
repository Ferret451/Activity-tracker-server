package com.bsuir.Activity_tracker_server.activity

import java.util.UUID
import kotlin.math.round

data class ActivitySummaryItem(
    val activityTypeId: UUID,
    val activityTypeName: String,
    val durationMinutes: Int,
    val distanceKm: Double,
    val burnedCaloriesKcal: Double,
)

object ActivitySummaryCalculator {
    fun calculate(items: List<ActivitySummaryItem>): ActivitySummaryResponse {
        val byType = items
            .groupBy { it.activityTypeId to it.activityTypeName }
            .map { (key, grouped) ->
                ActivityTypeSummaryResponse(
                    activityTypeId = key.first,
                    activityTypeName = key.second,
                    totalDurationMinutes = grouped.sumOf { it.durationMinutes },
                    totalDistanceKm = roundToTwo(grouped.sumOf { it.distanceKm }),
                    totalBurnedCaloriesKcal = roundToTwo(grouped.sumOf { it.burnedCaloriesKcal }),
                )
            }
            .sortedWith(
                compareByDescending<ActivityTypeSummaryResponse> { it.totalBurnedCaloriesKcal }
                    .thenBy { it.activityTypeName },
            )

        return ActivitySummaryResponse(
            totalDurationMinutes = items.sumOf { it.durationMinutes },
            totalDistanceKm = roundToTwo(items.sumOf { it.distanceKm }),
            totalBurnedCaloriesKcal = roundToTwo(items.sumOf { it.burnedCaloriesKcal }),
            byType = byType,
        )
    }

    private fun roundToTwo(value: Double): Double = round(value * 100) / 100
}

