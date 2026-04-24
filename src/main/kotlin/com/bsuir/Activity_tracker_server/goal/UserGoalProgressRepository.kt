package com.bsuir.Activity_tracker_server.goal

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface UserGoalProgressRepository : JpaRepository<UserGoalProgress, UUID> {
    fun findAllByGoal_IdOrderByRecordedAtDesc(goalId: UUID): List<UserGoalProgress>
}


