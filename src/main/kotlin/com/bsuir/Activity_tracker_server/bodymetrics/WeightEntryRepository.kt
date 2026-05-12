package com.bsuir.Activity_tracker_server.bodymetrics

import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime
import java.util.UUID

interface WeightEntryRepository : JpaRepository<WeightEntry, UUID> {
    fun findByIdAndUser_Id(id: UUID, userId: UUID): WeightEntry?

    fun findAllByUser_IdAndMeasuredAtBetweenOrderByMeasuredAtAsc(
        userId: UUID,
        from: OffsetDateTime,
        toExclusive: OffsetDateTime,
    ): List<WeightEntry>

    fun findAllByUser_IdOrderByMeasuredAtDesc(userId: UUID): List<WeightEntry>

    fun findTopByUser_IdOrderByMeasuredAtDesc(userId: UUID): WeightEntry?
}
