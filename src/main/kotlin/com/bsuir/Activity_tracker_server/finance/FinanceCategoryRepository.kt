package com.bsuir.Activity_tracker_server.finance

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FinanceCategoryRepository : JpaRepository<FinanceCategory, UUID> {
    fun findAllByUser_IdOrderByCreatedAtDesc(userId: UUID): List<FinanceCategory>
    fun findByIdAndUser_Id(id: UUID, userId: UUID): FinanceCategory?
    fun existsByUser_IdAndNameIgnoreCaseAndOperationType(
        userId: UUID,
        name: String,
        operationType: FinanceOperationType,
    ): Boolean
}

