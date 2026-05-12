package com.bsuir.Activity_tracker_server.activity

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ActivityTypeRepository : JpaRepository<ActivityType, UUID> {
    fun findAllByOrderByNameAsc(): List<ActivityType>

    fun existsByNameIgnoreCaseAndMet(name: String, met: Double): Boolean

    fun existsByNameIgnoreCaseAndMetAndIdNot(name: String, met: Double, id: UUID): Boolean
}
