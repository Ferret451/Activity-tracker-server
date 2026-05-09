package com.bsuir.Activity_tracker_server.finance.exception

import com.bsuir.Activity_tracker_server.finance.FinanceOperationType
import java.util.UUID

class FinanceCategoryNotFoundException(categoryId: UUID) :
    RuntimeException("Finance category with id '$categoryId' was not found")

class FinanceRecordNotFoundException(recordId: UUID) :
    RuntimeException("Finance record with id '$recordId' was not found")

class DuplicateFinanceCategoryException(name: String) :
    RuntimeException("Finance category '$name' already exists for this operation type")

class FinanceAccountNotFoundException(accountId: UUID) :
    RuntimeException("Finance account with id '$accountId' was not found")

class FinanceAccountInUseException(accountId: UUID) :
    RuntimeException("Finance account '$accountId' cannot be deleted because it has linked records")

class PrimaryFinanceAccountDeletionException(accountId: UUID) :
    RuntimeException("Finance account '$accountId' is primary and cannot be deleted")

class FinanceCategoryBranchMismatchException(expected: FinanceOperationType) :
    RuntimeException(
        "Category operation type must match the selected branch (${expected.name.lowercase()} only)",
    )

class FinanceRecordBranchMismatchException :
    RuntimeException("Finance record belongs to another operation branch")

