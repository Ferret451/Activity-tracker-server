package com.bsuir.Activity_tracker_server.userdata

import com.bsuir.Activity_tracker_server.common.AccessPolicy
import com.bsuir.Activity_tracker_server.finance.CreateFinanceAccountRequest
import com.bsuir.Activity_tracker_server.finance.CreateFinanceCategoryBranchRequest
import com.bsuir.Activity_tracker_server.finance.CreateFinanceRecordRequest
import com.bsuir.Activity_tracker_server.finance.FinanceOperationType
import com.bsuir.Activity_tracker_server.finance.FinanceRecordQueryFilter
import com.bsuir.Activity_tracker_server.finance.FinanceService
import com.bsuir.Activity_tracker_server.finance.UpdateFinanceAccountRequest
import com.bsuir.Activity_tracker_server.finance.UpdateFinanceCategoryBranchRequest
import com.bsuir.Activity_tracker_server.finance.UpdateFinanceRecordRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
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
@RequestMapping("/api/users/{userId}/finance")
class UserFinanceController(
    private val financeService: FinanceService,
    private val accessPolicy: AccessPolicy,
) {

    @GetMapping("/accounts")
    fun listAccounts(
        @PathVariable userId: UUID,
        authentication: Authentication,
    ) = doWithAccess(authentication, userId) { financeService.listAccounts(userId) }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    fun createAccount(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @Valid @RequestBody request: CreateFinanceAccountRequest,
    ) = doWithAccess(authentication, userId) { financeService.createAccount(userId, request) }

    @GetMapping("/accounts/{accountId}")
    fun getAccount(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable accountId: UUID,
    ) = doWithAccess(authentication, userId) { financeService.getAccount(userId, accountId) }

    @PutMapping("/accounts/{accountId}")
    fun updateAccount(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable accountId: UUID,
        @Valid @RequestBody request: UpdateFinanceAccountRequest,
    ) = doWithAccess(authentication, userId) { financeService.updateAccount(userId, accountId, request) }

    @DeleteMapping("/accounts/{accountId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAccount(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable accountId: UUID,
    ) {
        doWithAccess(authentication, userId) { financeService.deleteAccount(userId, accountId) }
    }

    @PostMapping("/income/categories")
    @ResponseStatus(HttpStatus.CREATED)
    fun createIncomeCategory(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @Valid @RequestBody request: CreateFinanceCategoryBranchRequest,
    ) = doWithAccess(authentication, userId) {
        financeService.createCategoryForBranch(userId, FinanceOperationType.INCOME, request)
    }

    @GetMapping("/income/categories")
    fun listIncomeCategories(
        @PathVariable userId: UUID,
        authentication: Authentication,
    ) = doWithAccess(authentication, userId) {
        financeService.getCategoriesForBranch(userId, FinanceOperationType.INCOME)
    }

    @PutMapping("/income/categories/{categoryId}")
    fun updateIncomeCategory(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable categoryId: UUID,
        @Valid @RequestBody request: UpdateFinanceCategoryBranchRequest,
    ) = doWithAccess(authentication, userId) {
        financeService.updateCategoryForBranch(userId, FinanceOperationType.INCOME, categoryId, request)
    }

    @DeleteMapping("/income/categories/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteIncomeCategory(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable categoryId: UUID,
    ) {
        doWithAccess(authentication, userId) {
            financeService.deleteCategoryForBranch(userId, FinanceOperationType.INCOME, categoryId)
        }
    }

    @PostMapping("/expense/categories")
    @ResponseStatus(HttpStatus.CREATED)
    fun createExpenseCategory(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @Valid @RequestBody request: CreateFinanceCategoryBranchRequest,
    ) = doWithAccess(authentication, userId) {
        financeService.createCategoryForBranch(userId, FinanceOperationType.EXPENSE, request)
    }

    @GetMapping("/expense/categories")
    fun listExpenseCategories(
        @PathVariable userId: UUID,
        authentication: Authentication,
    ) = doWithAccess(authentication, userId) {
        financeService.getCategoriesForBranch(userId, FinanceOperationType.EXPENSE)
    }

    @PutMapping("/expense/categories/{categoryId}")
    fun updateExpenseCategory(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable categoryId: UUID,
        @Valid @RequestBody request: UpdateFinanceCategoryBranchRequest,
    ) = doWithAccess(authentication, userId) {
        financeService.updateCategoryForBranch(userId, FinanceOperationType.EXPENSE, categoryId, request)
    }

    @DeleteMapping("/expense/categories/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteExpenseCategory(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable categoryId: UUID,
    ) {
        doWithAccess(authentication, userId) {
            financeService.deleteCategoryForBranch(userId, FinanceOperationType.EXPENSE, categoryId)
        }
    }

    @PostMapping("/income/records")
    @ResponseStatus(HttpStatus.CREATED)
    fun createIncomeRecord(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @Valid @RequestBody request: CreateFinanceRecordRequest,
    ) = doWithAccess(authentication, userId) {
        financeService.createRecordForBranch(userId, FinanceOperationType.INCOME, request)
    }

    @GetMapping("/income/records")
    fun listIncomeRecords(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(required = false) categoryId: UUID?,
        @RequestParam(required = false) accountId: UUID?,
    ) = doWithAccess(authentication, userId) {
        financeService.getRecordsForBranch(
            userId,
            FinanceOperationType.INCOME,
            FinanceRecordQueryFilter(from = from, to = to, categoryId = categoryId, accountId = accountId),
        )
    }

    @GetMapping("/income/records/{recordId}")
    fun getIncomeRecord(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable recordId: UUID,
    ) = doWithAccess(authentication, userId) {
        financeService.getRecordForBranch(userId, FinanceOperationType.INCOME, recordId)
    }

    @PutMapping("/income/records/{recordId}")
    fun updateIncomeRecord(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable recordId: UUID,
        @Valid @RequestBody request: UpdateFinanceRecordRequest,
    ) = doWithAccess(authentication, userId) {
        financeService.updateRecordForBranch(userId, FinanceOperationType.INCOME, recordId, request)
    }

    @DeleteMapping("/income/records/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteIncomeRecord(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable recordId: UUID,
    ) {
        doWithAccess(authentication, userId) {
            financeService.deleteRecordForBranch(userId, FinanceOperationType.INCOME, recordId)
        }
    }

    @PostMapping("/expense/records")
    @ResponseStatus(HttpStatus.CREATED)
    fun createExpenseRecord(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @Valid @RequestBody request: CreateFinanceRecordRequest,
    ) = doWithAccess(authentication, userId) {
        financeService.createRecordForBranch(userId, FinanceOperationType.EXPENSE, request)
    }

    @GetMapping("/expense/records")
    fun listExpenseRecords(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(required = false) categoryId: UUID?,
        @RequestParam(required = false) accountId: UUID?,
    ) = doWithAccess(authentication, userId) {
        financeService.getRecordsForBranch(
            userId,
            FinanceOperationType.EXPENSE,
            FinanceRecordQueryFilter(from = from, to = to, categoryId = categoryId, accountId = accountId),
        )
    }

    @GetMapping("/expense/records/{recordId}")
    fun getExpenseRecord(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable recordId: UUID,
    ) = doWithAccess(authentication, userId) {
        financeService.getRecordForBranch(userId, FinanceOperationType.EXPENSE, recordId)
    }

    @PutMapping("/expense/records/{recordId}")
    fun updateExpenseRecord(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable recordId: UUID,
        @Valid @RequestBody request: UpdateFinanceRecordRequest,
    ) = doWithAccess(authentication, userId) {
        financeService.updateRecordForBranch(userId, FinanceOperationType.EXPENSE, recordId, request)
    }

    @DeleteMapping("/expense/records/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteExpenseRecord(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable recordId: UUID,
    ) {
        doWithAccess(authentication, userId) {
            financeService.deleteRecordForBranch(userId, FinanceOperationType.EXPENSE, recordId)
        }
    }

    @GetMapping("/report")
    fun getPeriodReport(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @RequestParam from: LocalDate,
        @RequestParam to: LocalDate,
        @RequestParam(required = false) accountId: UUID?,
    ) = doWithAccess(authentication, userId) { financeService.getPeriodReport(userId, from, to, accountId) }

    private inline fun <T> doWithAccess(authentication: Authentication, userId: UUID, block: () -> T): T {
        accessPolicy.requireSelfOrAdmin(authentication, userId)
        return block()
    }
}
