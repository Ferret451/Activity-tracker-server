package com.bsuir.Activity_tracker_server.activity

import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime
import java.util.UUID

data class StartWorkoutRequest(
    val activityTypeId: UUID,
)

data class StopWorkoutRequest(
    @field:DecimalMin(value = "0.0")
    val distanceKm: Double? = null,

    @field:Size(max = 500)
    val note: String? = null,

    val endedAt: OffsetDateTime? = null,
)

data class RoutePointInput(
    @field:DecimalMin(value = "-90.0")
    val latitude: Double,

    @field:DecimalMin(value = "-180.0")
    val longitude: Double,

    val recordedAt: OffsetDateTime? = null,
)

data class AppendWorkoutRoutePointsRequest(
    @field:NotEmpty
    @field:Valid
    val points: List<RoutePointInput>,
)

data class WorkoutSessionResponse(
    val id: UUID,
    val activityType: ActivityTypeResponse,
    val status: WorkoutSessionStatus,
    val startedAt: OffsetDateTime,
    val endedAt: OffsetDateTime?,
    val activityRecordId: UUID?,
    val note: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class WorkoutRoutePointResponse(
    val id: UUID,
    val pointSeq: Int,
    val latitude: Double,
    val longitude: Double,
    val recordedAt: OffsetDateTime,
)
