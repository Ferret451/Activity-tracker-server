package com.bsuir.Activity_tracker_server.goal

import com.bsuir.Activity_tracker_server.goal.exception.GoalNotFoundException
import com.bsuir.Activity_tracker_server.user.UserRepository
import com.bsuir.Activity_tracker_server.user.exception.UserNotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

@Service
class GoalService(
    private val userRepository: UserRepository,
    private val userGoalRepository: UserGoalRepository,
    private val userGoalProgressRepository: UserGoalProgressRepository,
) {

    @Transactional
    fun createGoal(userId: UUID, request: CreateGoalRequest): UserGoalResponse {
        val user = userRepository.findByIdOrNull(userId)
            ?: throw UserNotFoundException(userId)

        val goal = UserGoal(
            user = user,
            goalType = request.goalType,
            title = request.title.trim(),
            initialValue = request.initialValue,
            targetValue = request.targetValue,
            currentValue = request.initialValue,
            startDate = request.startDate,
            targetDate = request.targetDate,
        )

        return userGoalRepository.save(goal).toResponse()
    }

    @Transactional(readOnly = true)
    fun getGoals(userId: UUID): List<UserGoalResponse> =
        userGoalRepository.findAllByUser_IdOrderByCreatedAtDesc(userId)
            .map { it.toResponse() }

    @Transactional(readOnly = true)
    fun getGoal(userId: UUID, goalId: UUID): UserGoalResponse =
        findOwnedGoal(userId, goalId).toResponse()

    @Transactional
    fun updateStatus(userId: UUID, goalId: UUID, request: UpdateGoalStatusRequest): UserGoalResponse {
        val goal = findOwnedGoal(userId, goalId)
        goal.status = request.status
        return userGoalRepository.save(goal).toResponse()
    }

    @Transactional
    fun addProgress(userId: UUID, goalId: UUID, request: AddGoalProgressRequest): UserGoalResponse {
        val goal = findOwnedGoal(userId, goalId)

        val entry = UserGoalProgress(
            goal = goal,
            value = request.value,
            note = request.note?.trim()?.takeIf(String::isNotBlank),
            recordedAt = OffsetDateTime.now(ZoneOffset.UTC),
        )

        userGoalProgressRepository.save(entry)

        goal.currentValue = request.value
        if (GoalProgressCalculator.isGoalCompleted(goal.initialValue, goal.targetValue, goal.currentValue)) {
            goal.status = GoalStatus.COMPLETED
        }

        return userGoalRepository.save(goal).toResponse()
    }

    @Transactional(readOnly = true)
    fun getProgressEntries(userId: UUID, goalId: UUID): List<GoalProgressResponse> {
        findOwnedGoal(userId, goalId)

        return userGoalProgressRepository.findAllByGoal_IdOrderByRecordedAtDesc(goalId)
            .map {
                GoalProgressResponse(
                    id = it.id,
                    value = it.value,
                    note = it.note,
                    recordedAt = requireNotNull(it.recordedAt),
                )
            }
    }

    private fun findOwnedGoal(userId: UUID, goalId: UUID): UserGoal =
        userGoalRepository.findByIdAndUser_Id(goalId, userId)
            ?: throw GoalNotFoundException(goalId)

    private fun UserGoal.toResponse(): UserGoalResponse = UserGoalResponse(
        id = id,
        goalType = goalType,
        status = status,
        title = title,
        initialValue = initialValue,
        targetValue = targetValue,
        currentValue = currentValue,
        progressPercent = GoalProgressCalculator.calculateProgressPercent(initialValue, targetValue, currentValue),
        startDate = startDate,
        targetDate = targetDate,
        createdAt = requireNotNull(createdAt),
        updatedAt = requireNotNull(updatedAt),
    )
}





