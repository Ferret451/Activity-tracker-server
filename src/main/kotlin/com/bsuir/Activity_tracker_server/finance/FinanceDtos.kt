package com.bsuir.Activity_tracker_server.finance

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

data class CreateFinanceCategoryRequest(
    @field:NotBlank
    @field:Size(max = 80)
    val name: String,

    val operationType: FinanceOperationType,
)

data class FinanceCategoryResponse(
    val id: UUID,
    val name: String,
    val operationType: FinanceOperationType,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class CreateFinanceRecordRequest(
    val categoryId: UUID,

    @field:DecimalMin(value = "0.01")
    val amount: BigDecimal,

    @field:Size(max = 500)
    val note: String? = null,

    val occurredAt: OffsetDateTime? = null,
)

data class UpdateFinanceRecordRequest(
    val categoryId: UUID,

    @field:DecimalMin(value = "0.01")
    val amount: BigDecimal,

    @field:Size(max = 500)
    val note: String? = null,

    val occurredAt: OffsetDateTime,
)

data class FinanceRecordResponse(
    val id: UUID,
    val category: FinanceCategoryResponse,
    val operationType: FinanceOperationType,
    val amount: BigDecimal,
    val note: String?,
    val occurredAt: OffsetDateTime,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class FinanceCategorySummaryResponse(
    val categoryId: UUID,
    val categoryName: String,
    val operationType: FinanceOperationType,
    val totalAmount: BigDecimal,
)

data class FinanceSummaryResponse(
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal,
    val balance: BigDecimal,
    val byCategory: List<FinanceCategorySummaryResponse>,
)

data class FinanceQueryFilter(
    val from: LocalDate?,
    val to: LocalDate?,
    val categoryId: UUID?,
    val operationType: FinanceOperationType?,
)

