package com.bsuir.Activity_tracker_server.finance.exception

import java.util.UUID

class FinanceCategoryNotFoundException(categoryId: UUID) :
    RuntimeException("Finance category with id '$categoryId' was not found")

class FinanceRecordNotFoundException(recordId: UUID) :
    RuntimeException("Finance record with id '$recordId' was not found")

class DuplicateFinanceCategoryException(name: String) :
    RuntimeException("Finance category '$name' already exists for this operation type")

