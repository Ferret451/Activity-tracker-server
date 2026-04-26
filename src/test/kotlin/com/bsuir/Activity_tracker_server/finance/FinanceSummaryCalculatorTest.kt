package com.bsuir.Activity_tracker_server.finance

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.UUID

class FinanceSummaryCalculatorTest {

    @Test
    fun `calculate returns totals and grouped categories`() {
        val salaryCategoryId = UUID.randomUUID()
        val foodCategoryId = UUID.randomUUID()
        val transportCategoryId = UUID.randomUUID()

        val summary = FinanceSummaryCalculator.calculate(
            listOf(
                FinanceSummaryItem(BigDecimal("1200.00"), FinanceOperationType.INCOME, salaryCategoryId, "Salary"),
                FinanceSummaryItem(BigDecimal("300.00"), FinanceOperationType.INCOME, salaryCategoryId, "Salary"),
                FinanceSummaryItem(BigDecimal("80.00"), FinanceOperationType.EXPENSE, foodCategoryId, "Food"),
                FinanceSummaryItem(BigDecimal("20.00"), FinanceOperationType.EXPENSE, transportCategoryId, "Transport"),
            ),
        )

        assertEquals(BigDecimal("1500.00"), summary.totalIncome)
        assertEquals(BigDecimal("100.00"), summary.totalExpense)
        assertEquals(BigDecimal("1400.00"), summary.balance)
        assertEquals(3, summary.byCategory.size)
        assertEquals("Salary", summary.byCategory.first().categoryName)
        assertEquals(BigDecimal("1500.00"), summary.byCategory.first().totalAmount)
    }
}

