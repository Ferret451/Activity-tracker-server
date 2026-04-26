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

    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    fun createCategory(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreateFinanceCategoryRequest,
    ): FinanceCategoryResponse = financeService.createCategory(extractUserId(jwt), request)

    @GetMapping("/categories")
    fun getCategories(@AuthenticationPrincipal jwt: Jwt): List<FinanceCategoryResponse> =
        financeService.getCategories(extractUserId(jwt))

    @PostMapping("/records")
    @ResponseStatus(HttpStatus.CREATED)
    fun createRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreateFinanceRecordRequest,
    ): FinanceRecordResponse = financeService.createRecord(extractUserId(jwt), request)

    @GetMapping("/records")
    fun getRecords(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(required = false) categoryId: UUID?,
        @RequestParam(required = false) operationType: FinanceOperationType?,
    ): List<FinanceRecordResponse> =
        financeService.getRecords(
            userId = extractUserId(jwt),
            filter = FinanceQueryFilter(
                from = from,
                to = to,
                categoryId = categoryId,
                operationType = operationType,
            ),
        )

    @GetMapping("/records/{recordId}")
    fun getRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable recordId: UUID,
    ): FinanceRecordResponse = financeService.getRecord(extractUserId(jwt), recordId)

    @PutMapping("/records/{recordId}")
    fun updateRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable recordId: UUID,
        @Valid @RequestBody request: UpdateFinanceRecordRequest,
    ): FinanceRecordResponse = financeService.updateRecord(extractUserId(jwt), recordId, request)

    @DeleteMapping("/records/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable recordId: UUID,
    ) {
        financeService.deleteRecord(extractUserId(jwt), recordId)
    }

    @GetMapping("/summary")
    fun getSummary(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(required = false) categoryId: UUID?,
        @RequestParam(required = false) operationType: FinanceOperationType?,
    ): FinanceSummaryResponse =
        financeService.getSummary(
            userId = extractUserId(jwt),
            filter = FinanceQueryFilter(
                from = from,
                to = to,
                categoryId = categoryId,
                operationType = operationType,
            ),
        )

    private fun extractUserId(jwt: Jwt): UUID = UUID.fromString(jwt.subject)
}

