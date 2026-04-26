package com.bsuir.Activity_tracker_server.activity

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class CreateActivityTypeRequest(
    @field:NotBlank
    @field:Size(max = 80)
    val name: String,

    @field:DecimalMin(value = "0.1")
    val met: Double,
)

data class UpdateActivityTypeRequest(
    @field:NotBlank
    @field:Size(max = 80)
    val name: String,

    @field:DecimalMin(value = "0.1")
    val met: Double,
)

data class ActivityTypeResponse(
    val id: UUID,
    val name: String,
    val met: Double,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class CreateActivityRecordRequest(
    val activityTypeId: UUID,

    @field:Positive
    val durationMinutes: Int,

    @field:DecimalMin(value = "0.0")
    val distanceKm: Double? = null,

    @field:Size(max = 500)
    val note: String? = null,

    val occurredAt: OffsetDateTime? = null,
)

data class UpdateActivityRecordRequest(
    val activityTypeId: UUID,

    @field:Positive
    val durationMinutes: Int,

    @field:DecimalMin(value = "0.0")
    val distanceKm: Double? = null,

    @field:Size(max = 500)
    val note: String? = null,

    val occurredAt: OffsetDateTime,
)

data class ActivityRecordResponse(
    val id: UUID,
    val activityType: ActivityTypeResponse,
    val durationMinutes: Int,
    val distanceKm: Double?,
    val note: String?,
    val occurredAt: OffsetDateTime,
    val burnedCaloriesKcal: Double,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class ActivityTypeSummaryResponse(
    val activityTypeId: UUID,
    val activityTypeName: String,
    val totalDurationMinutes: Int,
    val totalDistanceKm: Double,
    val totalBurnedCaloriesKcal: Double,
)

data class ActivitySummaryResponse(
    val totalDurationMinutes: Int,
    val totalDistanceKm: Double,
    val totalBurnedCaloriesKcal: Double,
    val byType: List<ActivityTypeSummaryResponse>,
)

data class ActivityQueryFilter(
    val from: LocalDate?,
    val to: LocalDate?,
    val activityTypeId: UUID?,
)

