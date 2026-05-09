package com.bsuir.Activity_tracker_server.finance

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/me/finance")
class FinanceController(
    private val financeService: FinanceService,
) {

    @GetMapping("/accounts")
    fun listAccounts(@AuthenticationPrincipal jwt: Jwt): List<FinanceAccountResponse> =
        financeService.listAccounts(extractUserId(jwt))

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    fun createAccount(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreateFinanceAccountRequest,
    ): FinanceAccountResponse =
        financeService.createAccount(extractUserId(jwt), request)

    @GetMapping("/accounts/{accountId}")
    fun getAccount(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable accountId: UUID,
    ): FinanceAccountResponse =
        financeService.getAccount(extractUserId(jwt), accountId)

    @PutMapping("/accounts/{accountId}")
    fun updateAccount(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable accountId: UUID,
        @Valid @RequestBody request: UpdateFinanceAccountRequest,
    ): FinanceAccountResponse =
        financeService.updateAccount(extractUserId(jwt), accountId, request)

    @DeleteMapping("/accounts/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAccount(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable accountId: UUID,
    ) {
        financeService.deleteAccount(extractUserId(jwt), accountId)
    }

    @PostMapping("/income/categories")
    @ResponseStatus(HttpStatus.CREATED)
    fun createIncomeCategory(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreateFinanceCategoryBranchRequest,
    ): FinanceCategoryResponse =
        financeService.createCategoryForBranch(extractUserId(jwt), FinanceOperationType.INCOME, request)

    @GetMapping("/income/categories")
    fun listIncomeCategories(@AuthenticationPrincipal jwt: Jwt): List<FinanceCategoryResponse> =
        financeService.getCategoriesForBranch(extractUserId(jwt), FinanceOperationType.INCOME)

    @PostMapping("/expense/categories")
    @ResponseStatus(HttpStatus.CREATED)
    fun createExpenseCategory(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreateFinanceCategoryBranchRequest,
    ): FinanceCategoryResponse =
        financeService.createCategoryForBranch(extractUserId(jwt), FinanceOperationType.EXPENSE, request)

    @GetMapping("/expense/categories")
    fun listExpenseCategories(@AuthenticationPrincipal jwt: Jwt): List<FinanceCategoryResponse> =
        financeService.getCategoriesForBranch(extractUserId(jwt), FinanceOperationType.EXPENSE)

    @PostMapping("/income/records")
    @ResponseStatus(HttpStatus.CREATED)
    fun createIncomeRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreateFinanceRecordRequest,
    ): FinanceRecordResponse =
        financeService.createRecordForBranch(extractUserId(jwt), FinanceOperationType.INCOME, request)

    @GetMapping("/income/records")
    fun listIncomeRecords(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(required = false) categoryId: UUID?,
        @RequestParam(required = false) accountId: UUID?,
    ): List<FinanceRecordResponse> =
        financeService.getRecordsForBranch(
            extractUserId(jwt),
            FinanceOperationType.INCOME,
            FinanceRecordQueryFilter(from = from, to = to, categoryId = categoryId, accountId = accountId),
        )

    @GetMapping("/income/records/{recordId}")
    fun getIncomeRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable recordId: UUID,
    ): FinanceRecordResponse =
        financeService.getRecordForBranch(extractUserId(jwt), FinanceOperationType.INCOME, recordId)

    @PutMapping("/income/records/{recordId}")
    fun updateIncomeRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable recordId: UUID,
        @Valid @RequestBody request: UpdateFinanceRecordRequest,
    ): FinanceRecordResponse =
        financeService.updateRecordForBranch(extractUserId(jwt), FinanceOperationType.INCOME, recordId, request)

    @DeleteMapping("/income/records/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteIncomeRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable recordId: UUID,
    ) {
        financeService.deleteRecordForBranch(extractUserId(jwt), FinanceOperationType.INCOME, recordId)
    }

    @PostMapping("/expense/records")
    @ResponseStatus(HttpStatus.CREATED)
    fun createExpenseRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreateFinanceRecordRequest,
    ): FinanceRecordResponse =
        financeService.createRecordForBranch(extractUserId(jwt), FinanceOperationType.EXPENSE, request)

    @GetMapping("/expense/records")
    fun listExpenseRecords(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(required = false) categoryId: UUID?,
        @RequestParam(required = false) accountId: UUID?,
    ): List<FinanceRecordResponse> =
        financeService.getRecordsForBranch(
            extractUserId(jwt),
            FinanceOperationType.EXPENSE,
            FinanceRecordQueryFilter(from = from, to = to, categoryId = categoryId, accountId = accountId),
        )

    @GetMapping("/expense/records/{recordId}")
    fun getExpenseRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable recordId: UUID,
    ): FinanceRecordResponse =
        financeService.getRecordForBranch(extractUserId(jwt), FinanceOperationType.EXPENSE, recordId)

    @PutMapping("/expense/records/{recordId}")
    fun updateExpenseRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable recordId: UUID,
        @Valid @RequestBody request: UpdateFinanceRecordRequest,
    ): FinanceRecordResponse =
        financeService.updateRecordForBranch(extractUserId(jwt), FinanceOperationType.EXPENSE, recordId, request)

    @DeleteMapping("/expense/records/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteExpenseRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable recordId: UUID,
    ) {
        financeService.deleteRecordForBranch(extractUserId(jwt), FinanceOperationType.EXPENSE, recordId)
    }

    @GetMapping("/report")
    fun getPeriodReport(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
        @RequestParam(required = false) accountId: UUID?,
    ): FinancePeriodReportResponse =
        financeService.getPeriodReport(extractUserId(jwt), from, to, accountId)

    private fun extractUserId(jwt: Jwt): UUID = UUID.fromString(jwt.subject)
}
