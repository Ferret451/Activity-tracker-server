package com.bsuir.Activity_tracker_server.bodymetrics

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class CreateWeightEntryRequest(
    @field:DecimalMin(value = "0.001", inclusive = true)
    val weightKg: BigDecimal,

    val measuredAt: OffsetDateTime? = null,

    @field:Size(max = 500)
    val note: String? = null,
)

data class UpdateWeightEntryRequest(
    @field:DecimalMin(value = "0.001", inclusive = true)
    val weightKg: BigDecimal,

    val measuredAt: OffsetDateTime,

    @field:Size(max = 500)
    val note: String? = null,
)

data class WeightEntryResponse(
    val id: UUID,
    val weightKg: BigDecimal,
    val measuredAt: OffsetDateTime,
    val note: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class WeightStatsResponse(
    val from: LocalDate,
    val to: LocalDate,
    val count: Int,
    val minWeightKg: BigDecimal?,
    val maxWeightKg: BigDecimal?,
    val avgWeightKg: BigDecimal?,
    val entries: List<WeightEntryResponse>,
)

data class UpsertDailyStepsRequest(
    @field:Positive
    val steps: Int,

    @field:DecimalMin(value = "0.0")
    val burnedCaloriesKcal: Double,

    @field:Min(0)
    val walkingDurationMinutes: Int,
)

data class DailyStepsResponse(
    val date: LocalDate,
    val steps: Int,
    val burnedCaloriesKcal: Double,
    val walkingDurationMinutes: Int,
    val createdAt: OffsetDateTime?,
    val updatedAt: OffsetDateTime?,
)

data class DailyStepsStatsResponse(
    val from: LocalDate,
    val to: LocalDate,
    val days: List<DailyStepsResponse>,
)
