package com.bsuir.Activity_tracker_server.finance

import com.bsuir.Activity_tracker_server.finance.exception.DuplicateFinanceCategoryException
import com.bsuir.Activity_tracker_server.finance.exception.FinanceCategoryNotFoundException
import com.bsuir.Activity_tracker_server.finance.exception.FinanceRecordNotFoundException
import com.bsuir.Activity_tracker_server.user.UserRepository
import com.bsuir.Activity_tracker_server.user.exception.UserNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class FinanceService(
    private val userRepository: UserRepository,
    private val categoryRepository: FinanceCategoryRepository,
    private val recordRepository: FinanceRecordRepository,
) {

    @Transactional
    fun createCategory(userId: UUID, request: CreateFinanceCategoryRequest): FinanceCategoryResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)

        val normalizedName = request.name.trim()
        if (categoryRepository.existsByUser_IdAndNameIgnoreCaseAndOperationType(
                userId = userId,
                name = normalizedName,
                operationType = request.operationType,
            )
        ) {
            throw DuplicateFinanceCategoryException(normalizedName)
        }

        val category = FinanceCategory(
            user = user,
            name = normalizedName,
            operationType = request.operationType,
        )

        return categoryRepository.save(category).toResponse()
    }

    @Transactional(readOnly = true)
    fun getCategories(userId: UUID): List<FinanceCategoryResponse> =
        categoryRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)
            .map { it.toResponse() }

    @Transactional
    fun createRecord(userId: UUID, request: CreateFinanceRecordRequest): FinanceRecordResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)

        val category = findOwnedCategory(userId, request.categoryId)

        val record = FinanceRecord(
            user = user,
            category = category,
            operationType = category.operationType,
            amount = request.amount,
            note = request.note?.trim()?.takeIf(String::isNotBlank),
            occurredAt = request.occurredAt ?: OffsetDateTime.now(ZoneOffset.UTC),
        )

        return recordRepository.save(record).toResponse()
    }

    @Transactional(readOnly = true)
    fun getRecords(userId: UUID, filter: FinanceQueryFilter): List<FinanceRecordResponse> {
        val records = getFilteredRecords(userId, filter)
        return records.map { it.toResponse() }
    }

    @Transactional(readOnly = true)
    fun getRecord(userId: UUID, recordId: UUID): FinanceRecordResponse =
        findOwnedRecord(userId, recordId).toResponse()

    @Transactional
    fun updateRecord(userId: UUID, recordId: UUID, request: UpdateFinanceRecordRequest): FinanceRecordResponse {
        val record = findOwnedRecord(userId, recordId)
        val category = findOwnedCategory(userId, request.categoryId)

        record.category = category
        record.operationType = category.operationType
        record.amount = request.amount
        record.note = request.note?.trim()?.takeIf(String::isNotBlank)
        record.occurredAt = request.occurredAt

        return recordRepository.save(record).toResponse()
    }

    @Transactional
    fun deleteRecord(userId: UUID, recordId: UUID) {
        val record = findOwnedRecord(userId, recordId)
        recordRepository.delete(record)
    }

    @Transactional(readOnly = true)
    fun getSummary(userId: UUID, filter: FinanceQueryFilter): FinanceSummaryResponse {
        val records = getFilteredRecords(userId, filter)

        val summaryItems = records.map {
            FinanceSummaryItem(
                amount = it.amount,
                operationType = it.operationType,
                categoryId = it.category.id,
                categoryName = it.category.name,
            )
        }

        return FinanceSummaryCalculator.calculate(summaryItems)
    }

    private fun getFilteredRecords(userId: UUID, filter: FinanceQueryFilter): List<FinanceRecord> {
        validateDateRange(filter.from, filter.to)

        if (filter.categoryId != null) {
            findOwnedCategory(userId, filter.categoryId)
        }

        val fromDateTime = filter.from?.atStartOfDay()?.atOffset(ZoneOffset.UTC)
        val toDateTimeExclusive = filter.to?.plusDays(1)?.atStartOfDay()?.atOffset(ZoneOffset.UTC)

        return recordRepository.findAllByUser_IdOrderByOccurredAtDesc(userId)
            .asSequence()
            .filter { fromDateTime == null || !it.occurredAt.isBefore(fromDateTime) }
            .filter { toDateTimeExclusive == null || it.occurredAt.isBefore(toDateTimeExclusive) }
            .filter { filter.categoryId == null || it.category.id == filter.categoryId }
            .filter { filter.operationType == null || it.operationType == filter.operationType }
            .toList()
    }

    private fun validateDateRange(from: LocalDate?, to: LocalDate?) {
        if (from != null && to != null && from.isAfter(to)) {
            throw IllegalArgumentException("Parameter 'from' must be before or equal to 'to'")
        }
    }

    private fun findOwnedCategory(userId: UUID, categoryId: UUID): FinanceCategory =
        categoryRepository.findByIdAndUser_Id(categoryId, userId)
            ?: throw FinanceCategoryNotFoundException(categoryId)

    private fun findOwnedRecord(userId: UUID, recordId: UUID): FinanceRecord =
        recordRepository.findByIdAndUser_Id(recordId, userId)
            ?: throw FinanceRecordNotFoundException(recordId)

    private fun FinanceCategory.toResponse(): FinanceCategoryResponse = FinanceCategoryResponse(
        id = id,
        name = name,
        operationType = operationType,
        createdAt = requireNotNull(createdAt),
        updatedAt = requireNotNull(updatedAt),
    )

    private fun FinanceRecord.toResponse(): FinanceRecordResponse = FinanceRecordResponse(
        id = id,
        category = category.toResponse(),
        operationType = operationType,
        amount = amount,
        note = note,
        occurredAt = occurredAt,
        createdAt = requireNotNull(createdAt),
        updatedAt = requireNotNull(updatedAt),
    )
}

