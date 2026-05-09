package com.bsuir.Activity_tracker_server.finance

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface FinanceRecordRepository :
    JpaRepository<FinanceRecord, UUID>,
    JpaSpecificationExecutor<FinanceRecord> {
    @Query(
        """
        SELECT r FROM FinanceRecord r
        JOIN FETCH r.category
        JOIN FETCH r.account
        WHERE r.id = :id AND r.user.id = :userId
        """,
    )
    fun findByIdAndUser_IdWithDetails(
        @Param("id") id: UUID,
        @Param("userId") userId: UUID,
    ): FinanceRecord?

    fun countByAccountId(accountId: UUID): Long
}
