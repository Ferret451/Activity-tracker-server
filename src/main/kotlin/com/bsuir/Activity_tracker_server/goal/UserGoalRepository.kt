package com.bsuir.Activity_tracker_server.goal

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserGoalRepository : JpaRepository<UserGoal, UUID> {
    fun findAllByUser_IdOrderByCreatedAtDesc(userId: UUID): List<UserGoal>
    fun findByIdAndUser_Id(id: UUID, userId: UUID): UserGoal?
    fun findFirstByUser_IdAndGoalTypeAndStatusOrderByCreatedAtDesc(
        userId: UUID,
        goalType: GoalType,
        status: GoalStatus,
    ): UserGoal?
}


