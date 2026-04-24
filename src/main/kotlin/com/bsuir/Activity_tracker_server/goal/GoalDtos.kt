package com.bsuir.Activity_tracker_server.goal

import jakarta.validation.constraints.FutureOrPresent
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.PositiveOrZero
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class CreateGoalRequest(
    val goalType: GoalType,

    @field:NotBlank
    @field:Size(max = 120)
    val title: String,

    @field:PositiveOrZero
    val initialValue: Double,

    @field:PositiveOrZero
    val targetValue: Double,

    val startDate: LocalDate,

    @field:FutureOrPresent
    val targetDate: LocalDate? = null,
) {
    @Suppress("unused")
    @get:AssertTrue(message = "targetDate must be greater than or equal to startDate")
    val isDateRangeValid: Boolean
        get() = targetDate == null || !targetDate.isBefore(startDate)
}

data class UpdateGoalStatusRequest(
    val status: GoalStatus,
)

data class AddGoalProgressRequest(
    @field:PositiveOrZero
    val value: Double,

    @field:Size(max = 500)
    val note: String? = null,
)

data class GoalProgressResponse(
    val id: UUID,
    val value: Double,
    val note: String?,
    val recordedAt: OffsetDateTime,
)

data class UserGoalResponse(
    val id: UUID,
    val goalType: GoalType,
    val status: GoalStatus,
    val title: String,
    val initialValue: Double,
    val targetValue: Double,
    val currentValue: Double,
    val progressPercent: Double,
    val startDate: LocalDate,
    val targetDate: LocalDate?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)



