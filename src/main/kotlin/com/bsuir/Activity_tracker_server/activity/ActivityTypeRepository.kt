package com.bsuir.Activity_tracker_server.activity

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ActivityTypeRepository : JpaRepository<ActivityType, UUID> {
    fun findAllByUser_IdOrderByCreatedAtDesc(userId: UUID): List<ActivityType>
    fun findByIdAndUser_Id(id: UUID, userId: UUID): ActivityType?
    fun existsByUser_IdAndNameIgnoreCase(userId: UUID, name: String): Boolean
}

