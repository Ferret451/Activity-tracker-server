package com.bsuir.Activity_tracker_server.activity

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ActivityRecordRepository : JpaRepository<ActivityRecord, UUID> {
    fun findByIdAndUser_Id(id: UUID, userId: UUID): ActivityRecord?
    fun findAllByUser_IdOrderByOccurredAtDesc(userId: UUID): List<ActivityRecord>
    fun existsByActivityType_IdAndUser_Id(activityTypeId: UUID, userId: UUID): Boolean
}

