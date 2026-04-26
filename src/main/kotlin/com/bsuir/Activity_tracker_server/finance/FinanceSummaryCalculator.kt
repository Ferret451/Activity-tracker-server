package com.bsuir.Activity_tracker_server.finance

import java.math.BigDecimal
import java.util.UUID

data class FinanceSummaryItem(
    val amount: BigDecimal,
    val operationType: FinanceOperationType,
    val categoryId: UUID,
    val categoryName: String,
)

object FinanceSummaryCalculator {
    fun calculate(items: List<FinanceSummaryItem>): FinanceSummaryResponse {
        val totalIncome = items
            .filter { it.operationType == FinanceOperationType.INCOME }
            .fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }

        val totalExpense = items
            .filter { it.operationType == FinanceOperationType.EXPENSE }
            .fold(BigDecimal.ZERO) { acc, item -> acc + item.amount }

        val byCategory = items
            .groupBy { Triple(it.categoryId, it.categoryName, it.operationType) }
            .map { (key, categoryItems) ->
                FinanceCategorySummaryResponse(
                    categoryId = key.first,
                    categoryName = key.second,
                    operationType = key.third,
                    totalAmount = categoryItems.fold(BigDecimal.ZERO) { acc, item -> acc + item.amount },
                )
            }
            .sortedWith(
                compareByDescending<FinanceCategorySummaryResponse> { it.totalAmount }
                    .thenBy { it.categoryName },
            )

        return FinanceSummaryResponse(
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            balance = totalIncome - totalExpense,
            byCategory = byCategory,
        )
    }
}

