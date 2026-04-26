package com.bsuir.Activity_tracker_server.finance

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FinanceRecordRepository : JpaRepository<FinanceRecord, UUID> {
    fun findByIdAndUser_Id(id: UUID, userId: UUID): FinanceRecord?
    fun findAllByUser_IdOrderByOccurredAtDesc(userId: UUID): List<FinanceRecord>
}

