package com.bsuir.Activity_tracker_server.bodymetrics

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate
import java.util.UUID

interface DailyStepCountRepository : JpaRepository<DailyStepCount, DailyStepCountId> {
    fun findByUserIdAndStepDate(userId: UUID, stepDate: LocalDate): DailyStepCount?

    fun findAllByUserIdAndStepDateBetweenOrderByStepDateAsc(
        userId: UUID,
        from: LocalDate,
        to: LocalDate,
    ): List<DailyStepCount>
}
