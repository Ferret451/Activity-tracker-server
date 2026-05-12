package com.bsuir.Activity_tracker_server.finance

import com.bsuir.Activity_tracker_server.finance.exception.DuplicateFinanceCategoryException
import com.bsuir.Activity_tracker_server.finance.exception.FinanceAccountInUseException
import com.bsuir.Activity_tracker_server.finance.exception.FinanceAccountNotFoundException
import com.bsuir.Activity_tracker_server.finance.exception.FinanceCategoryBranchMismatchException
import com.bsuir.Activity_tracker_server.finance.exception.FinanceCategoryInUseException
import com.bsuir.Activity_tracker_server.finance.exception.FinanceCategoryNotFoundException
import com.bsuir.Activity_tracker_server.finance.exception.FinanceRecordBranchMismatchException
import com.bsuir.Activity_tracker_server.finance.exception.FinanceRecordNotFoundException
import com.bsuir.Activity_tracker_server.finance.exception.PrimaryFinanceAccountDeletionException
import com.bsuir.Activity_tracker_server.user.UserRepository
import com.bsuir.Activity_tracker_server.user.exception.UserNotFoundException
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class FinanceService(
    private val userRepository: UserRepository,
    private val categoryRepository: FinanceCategoryRepository,
    private val recordRepository: FinanceRecordRepository,
    private val accountRepository: FinanceAccountRepository,
) {

    @Transactional(readOnly = true)
    fun listAccounts(userId: UUID): List<FinanceAccountResponse> =
        accountRepository.findAllByUser_IdOrderByIsPrimaryDescCreatedAtDesc(userId)
            .map { it.toAccountResponse() }

    @Transactional
    fun createAccount(userId: UUID, request: CreateFinanceAccountRequest): FinanceAccountResponse {
        val user = userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)

        val hadAccountsBeforeSave = accountRepository.existsByUser_Id(userId)

        val account =
            FinanceAccount(
                user = user,
                name = request.name.trim(),
                isPrimary = false,
            ).let { accountRepository.save(it) }

        val shouldBePrimary = !hadAccountsBeforeSave || request.setAsPrimary
        if (shouldBePrimary) {
            if (hadAccountsBeforeSave) {
                accountRepository.clearPrimaryFlagForUserExcept(userId, account.id)
            }
            account.isPrimary = true
            accountRepository.save(account)
        }

        return account.toAccountResponse()
    }

    @Transactional(readOnly = true)
    fun getAccount(userId: UUID, accountId: UUID): FinanceAccountResponse =
        findOwnedAccount(userId, accountId).toAccountResponse()

    @Transactional
    fun updateAccount(userId: UUID, accountId: UUID, request: UpdateFinanceAccountRequest): FinanceAccountResponse {
        val account = findOwnedAccount(userId, accountId)

        account.name = request.name.trim()

        if (request.setAsPrimary) {
            accountRepository.clearPrimaryFlagForUserExcept(userId, account.id)
            account.isPrimary = true
        }

        return accountRepository.save(account).toAccountResponse()
    }

    @Transactional
    fun deleteAccount(userId: UUID, accountId: UUID) {
        val account = findOwnedAccount(userId, accountId)
        if (account.isPrimary) {
            throw PrimaryFinanceAccountDeletionException(accountId)
        }
        if (recordRepository.countByAccountId(accountId) > 0) {
            throw FinanceAccountInUseException(accountId)
        }
        accountRepository.delete(account)
    }

    @Transactional
    fun createCategoryForBranch(
        userId: UUID,
        branch: FinanceOperationType,
        request: CreateFinanceCategoryBranchRequest,
    ): FinanceCategoryResponse {
        val user = userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)

        val normalizedName = request.name.trim()
        if (categoryRepository.existsByUser_IdAndNameIgnoreCaseAndOperationType(
                userId = userId,
                name = normalizedName,
                operationType = branch,
            )
        ) {
            throw DuplicateFinanceCategoryException(normalizedName)
        }

        val category =
            FinanceCategory(
                user = user,
                name = normalizedName,
                operationType = branch,
            )
        category.iconId = request.iconId
        category.colorId = request.colorId

        return categoryRepository.save(category).toCategoryResponse()
    }

    @Transactional(readOnly = true)
    fun getCategoriesForBranch(userId: UUID, branch: FinanceOperationType): List<FinanceCategoryResponse> =
        categoryRepository.findAllByUser_IdAndOperationTypeOrderByCreatedAtDesc(userId, branch)
            .map { it.toCategoryResponse() }

    @Transactional
    fun createRecordForBranch(
        userId: UUID,
        branch: FinanceOperationType,
        request: CreateFinanceRecordRequest,
    ): FinanceRecordResponse {
        val user = userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)

        val category = findOwnedCategory(userId, request.categoryId)
        requireCategoryMatchesBranch(category, branch)

        val account = resolveAccountForNewRecord(userId, request.accountId)

        val record =
            FinanceRecord(
                user = user,
                category = category,
                account = account,
                operationType = category.operationType,
                amount = request.amount,
                note = request.note?.trim()?.takeIf(String::isNotBlank),
                occurredAt = request.occurredAt ?: OffsetDateTime.now(ZoneOffset.UTC),
            )

        persistBalanceContribution(account.id, FinanceBalanceDelta.signedContribution(record.operationType, record.amount))

        return recordRepository.save(record).let { refreshed ->
            recordRepository.findByIdAndUser_IdWithDetails(refreshed.id, userId)!!
                .toRecordResponse()
        }
    }

    @Transactional(readOnly = true)
    fun getRecordsForBranch(
        userId: UUID,
        branch: FinanceOperationType,
        filter: FinanceRecordQueryFilter,
    ): List<FinanceRecordResponse> {
        val records = queryRecords(userId, branch, filter)
        return records.map { it.toRecordResponse() }
    }

    @Transactional(readOnly = true)
    fun getRecordForBranch(userId: UUID, branch: FinanceOperationType, recordId: UUID): FinanceRecordResponse {
        val record = findOwnedRecord(userId, recordId)
        requireRecordMatchesBranch(record, branch)
        return record.toRecordResponse()
    }

    @Transactional
    fun updateRecordForBranch(
        userId: UUID,
        branch: FinanceOperationType,
        recordId: UUID,
        request: UpdateFinanceRecordRequest,
    ): FinanceRecordResponse {
        val record = findOwnedRecord(userId, recordId)
        requireRecordMatchesBranch(record, branch)

        val oldAccountId = record.account.id
        val oldOp = record.operationType
        val oldAmount = record.amount

        val category = findOwnedCategory(userId, request.categoryId)
        requireCategoryMatchesBranch(category, branch)

        val newAccount =
            resolveAccountForUpdate(userId, request.accountId, defaultToRecordAccountId = oldAccountId)

        val oldSigned = FinanceBalanceDelta.signedContribution(oldOp, oldAmount)
        val newSigned =
            FinanceBalanceDelta.signedContribution(category.operationType, request.amount)

        when {
            oldAccountId == newAccount.id ->
                persistBalanceContribution(
                    accountId = oldAccountId,
                    delta = newSigned.subtract(oldSigned),
                )

            else -> {
                persistBalanceContribution(accountId = oldAccountId, delta = oldSigned.negate())
                persistBalanceContribution(accountId = newAccount.id, delta = newSigned)
            }
        }

        record.category = category
        record.account = newAccount
        record.operationType = category.operationType
        record.amount = request.amount
        record.note = request.note?.trim()?.takeIf(String::isNotBlank)
        record.occurredAt = request.occurredAt

        recordRepository.save(record)

        return recordRepository.findByIdAndUser_IdWithDetails(recordId, userId)!!
            .toRecordResponse()
    }

    @Transactional
    fun deleteRecordForBranch(userId: UUID, branch: FinanceOperationType, recordId: UUID) {
        val record = findOwnedRecord(userId, recordId)
        requireRecordMatchesBranch(record, branch)

        persistBalanceContribution(
            accountId = record.account.id,
            delta =
                FinanceBalanceDelta
                    .signedContribution(record.operationType, record.amount)
                    .negate(),
        )
        recordRepository.delete(record)
    }

    @Transactional(readOnly = true)
    fun getPeriodReport(
        userId: UUID,
        from: LocalDate,
        to: LocalDate,
        accountId: UUID?,
    ): FinancePeriodReportResponse {
        validateDateRangeInclusive(from, to)
        accountId?.let { findOwnedAccount(userId, it) }

        val records =
            queryRecords(
                userId = userId,
                branch = null,
                filter =
                    FinanceRecordQueryFilter(
                        from = from,
                        to = to,
                        categoryId = null,
                        accountId = accountId,
                    ),
            )

        val summaryItems =
            records.map {
                FinanceSummaryItem(
                    amount = it.amount,
                    operationType = it.operationType,
                    categoryId = it.category.id,
                    categoryName = it.category.name,
                )
            }
        val summary = FinanceSummaryCalculator.calculate(summaryItems)

        val incomeRecords =
            records
                .filter { it.operationType == FinanceOperationType.INCOME }
                .map { it.toRecordResponse() }
        val expenseRecords =
            records
                .filter { it.operationType == FinanceOperationType.EXPENSE }
                .map { it.toRecordResponse() }

        return FinancePeriodReportResponse(
            from = from,
            to = to,
            totalIncome = summary.totalIncome,
            totalExpense = summary.totalExpense,
            net = summary.balance,
            byCategory = summary.byCategory,
            incomeRecords = incomeRecords,
            expenseRecords = expenseRecords,
        )
    }

    @Transactional
    fun updateCategoryForBranch(
        userId: UUID,
        branch: FinanceOperationType,
        categoryId: UUID,
        request: UpdateFinanceCategoryBranchRequest,
    ): FinanceCategoryResponse {
        val category = findOwnedCategory(userId, categoryId)
        requireCategoryMatchesBranch(category, branch)
        val normalizedName = request.name.trim()
        if (!category.name.equals(normalizedName, ignoreCase = true) &&
            categoryRepository.existsByUser_IdAndNameIgnoreCaseAndOperationType(
                userId = userId,
                name = normalizedName,
                operationType = branch,
            )
        ) {
            throw DuplicateFinanceCategoryException(normalizedName)
        }
        category.name = normalizedName
        category.iconId = request.iconId
        category.colorId = request.colorId
        return categoryRepository.save(category).toCategoryResponse()
    }

    @Transactional
    fun deleteCategoryForBranch(userId: UUID, branch: FinanceOperationType, categoryId: UUID) {
        val category = findOwnedCategory(userId, categoryId)
        requireCategoryMatchesBranch(category, branch)
        if (recordRepository.countByCategory_Id(categoryId) > 0L) {
            throw FinanceCategoryInUseException(categoryId)
        }
        categoryRepository.delete(category)
    }

    private fun persistBalanceContribution(accountId: UUID, delta: BigDecimal) {
        if (delta.compareTo(BigDecimal.ZERO) == 0) return
        val account =
            accountRepository.findByIdOrNull(accountId)
                ?: throw FinanceAccountNotFoundException(accountId)
        account.balance = account.balance.add(delta)
        accountRepository.save(account)
    }

    private fun resolveAccountForNewRecord(userId: UUID, requestedAccountId: UUID?): FinanceAccount =
        requestedAccountId?.let { findOwnedAccount(userId, it) }
            ?: accountRepository.findByUser_IdAndIsPrimaryIsTrue(userId)
                ?: createPrimaryAccountForUser(userId)

    private fun resolveAccountForUpdate(
        userId: UUID,
        requestedAccountId: UUID?,
        defaultToRecordAccountId: UUID,
    ): FinanceAccount {
        val targetId =
            requestedAccountId ?: defaultToRecordAccountId
        return findOwnedAccount(userId, targetId)
    }

    private fun createPrimaryAccountForUser(userId: UUID): FinanceAccount {
        val user = userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)

        accountRepository.clearAllPrimaryFlagsForUser(userId)

        val primary =
            FinanceAccount(
                user = user,
                name = PRIMARY_ACCOUNT_FALLBACK_NAME,
                isPrimary = true,
            )
        return accountRepository.save(primary)
    }

    private fun queryRecords(
        userId: UUID,
        branch: FinanceOperationType?,
        filter: FinanceRecordQueryFilter,
    ): List<FinanceRecord> {
        validateDateRangeInclusiveNullable(filter.from, filter.to)

        filter.categoryId?.let { findOwnedCategory(userId, it) }
        filter.accountId?.let { findOwnedAccount(userId, it) }

        val (fromStart, toExclusive) = toOffsetBounds(filter.from, filter.to)

        val spec =
            Specification.where(FinanceRecordSpecifications.forUser(userId))
                .and(FinanceRecordSpecifications.withBranch(branch))
                .and(FinanceRecordSpecifications.occurredFrom(fromStart))
                .and(FinanceRecordSpecifications.occurredUntil(toExclusive))
                .and(FinanceRecordSpecifications.category(filter.categoryId))
                .and(FinanceRecordSpecifications.account(filter.accountId))

        return recordRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "occurredAt"))
    }

    private fun findOwnedCategory(userId: UUID, categoryId: UUID): FinanceCategory =
        categoryRepository.findByIdAndUser_Id(categoryId, userId)
            ?: throw FinanceCategoryNotFoundException(categoryId)

    private fun findOwnedRecord(userId: UUID, recordId: UUID): FinanceRecord =
        recordRepository.findByIdAndUser_IdWithDetails(recordId, userId)
            ?: throw FinanceRecordNotFoundException(recordId)

    private fun findOwnedAccount(userId: UUID, accountId: UUID): FinanceAccount =
        accountRepository.findByIdAndUser_Id(accountId, userId)
            ?: throw FinanceAccountNotFoundException(accountId)

    private fun requireCategoryMatchesBranch(category: FinanceCategory, branch: FinanceOperationType) {
        if (category.operationType != branch) {
            throw FinanceCategoryBranchMismatchException(branch)
        }
    }

    private fun requireRecordMatchesBranch(record: FinanceRecord, branch: FinanceOperationType) {
        if (record.operationType != branch) {
            throw FinanceRecordBranchMismatchException()
        }
    }

    private fun validateDateRangeInclusiveNullable(from: LocalDate?, to: LocalDate?) {
        if (from != null && to != null && from.isAfter(to)) {
            throw IllegalArgumentException("Parameter 'from' must be before or equal to 'to'")
        }
    }

    private fun validateDateRangeInclusive(from: LocalDate, to: LocalDate) {
        if (from.isAfter(to)) {
            throw IllegalArgumentException("Parameter 'from' must be before or equal to 'to'")
        }
    }

    private fun toOffsetBounds(
        from: LocalDate?,
        to: LocalDate?,
    ): Pair<OffsetDateTime?, OffsetDateTime?> {
        val fromStart = from?.atStartOfDay()?.atOffset(ZoneOffset.UTC)
        val toExclusive = to?.plusDays(1)?.atStartOfDay()?.atOffset(ZoneOffset.UTC)
        return fromStart to toExclusive
    }

    private fun FinanceCategory.toCategoryResponse(): FinanceCategoryResponse = FinanceCategoryResponse(
        id = id,
        name = name,
        operationType = operationType,
        iconId = iconId,
        colorId = colorId,
        createdAt = requireNotNull(createdAt),
        updatedAt = requireNotNull(updatedAt),
    )

    private fun FinanceAccount.toAccountResponse(): FinanceAccountResponse = FinanceAccountResponse(
        id = id,
        name = name,
        isPrimary = isPrimary,
        balance = balance,
        createdAt = requireNotNull(createdAt),
        updatedAt = requireNotNull(updatedAt),
    )

    private fun FinanceAccount.toSummaryResponse(): FinanceAccountSummaryResponse =
        FinanceAccountSummaryResponse(id = id, name = name)

    private fun FinanceRecord.toRecordResponse(): FinanceRecordResponse = FinanceRecordResponse(
        id = id,
        account = account.toSummaryResponse(),
        category = category.toCategoryResponse(),
        operationType = operationType,
        amount = amount,
        note = note,
        occurredAt = occurredAt,
        createdAt = requireNotNull(createdAt),
        updatedAt = requireNotNull(updatedAt),
    )

    private companion object {
        const val PRIMARY_ACCOUNT_FALLBACK_NAME: String = "Основной"
    }
}
