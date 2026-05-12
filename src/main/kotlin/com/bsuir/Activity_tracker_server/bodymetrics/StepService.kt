package com.bsuir.Activity_tracker_server.bodymetrics

import com.bsuir.Activity_tracker_server.user.UserRepository
import com.bsuir.Activity_tracker_server.user.exception.UserNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.UUID

@Service
class StepService(
    private val userRepository: UserRepository,
    private val dailyStepCountRepository: DailyStepCountRepository,
) {

    @Transactional
    fun upsertSteps(userId: UUID, date: LocalDate, request: UpsertDailyStepsRequest): DailyStepsResponse {
        val user = userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)
        val existing = dailyStepCountRepository.findByUserIdAndStepDate(userId, date)
        val entity =
            if (existing == null) {
                DailyStepCount(
                    userId = user.id,
                    stepDate = date,
                    steps = request.steps,
                )
            } else {
                existing.steps = request.steps
                existing
            }
        val saved = dailyStepCountRepository.save(entity)
        return DailyStepsResponse(
            date = saved.stepDate,
            steps = saved.steps,
            createdAt = saved.createdAt,
            updatedAt = saved.updatedAt,
        )
    }

    @Transactional(readOnly = true)
    fun getDaily(userId: UUID, date: LocalDate): DailyStepsResponse {
        userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)
        val row = dailyStepCountRepository.findByUserIdAndStepDate(userId, date)
        return DailyStepsResponse(
            date = date,
            steps = row?.steps ?: 0,
            createdAt = row?.createdAt,
            updatedAt = row?.updatedAt,
        )
    }

    @Transactional(readOnly = true)
    fun getStats(userId: UUID, from: LocalDate, to: LocalDate): DailyStepsStatsResponse {
        userRepository.findByIdOrNull(userId) ?: throw UserNotFoundException(userId)
        if (from.isAfter(to)) {
            throw IllegalArgumentException("Parameter 'from' must be before or equal to 'to'")
        }
        val rows = dailyStepCountRepository.findAllByUserIdAndStepDateBetweenOrderByStepDateAsc(userId, from, to)
        val byDate = rows.associateBy { it.stepDate }
        val days = mutableListOf<DailyStepsResponse>()
        var d = from
        while (!d.isAfter(to)) {
            val row = byDate[d]
            days.add(
                DailyStepsResponse(
                    date = d,
                    steps = row?.steps ?: 0,
                    createdAt = row?.createdAt,
                    updatedAt = row?.updatedAt,
                ),
            )
            d = d.plusDays(1)
        }
        return DailyStepsStatsResponse(from = from, to = to, days = days)
    }
}
