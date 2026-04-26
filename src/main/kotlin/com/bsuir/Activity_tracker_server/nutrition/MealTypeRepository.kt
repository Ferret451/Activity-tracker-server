package com.bsuir.Activity_tracker_server.nutrition

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MealTypeRepository : JpaRepository<MealType, UUID> {
    fun findByIdAndUser_Id(id: UUID, userId: UUID): MealType?
    fun findAllByUser_IdOrderByCreatedAtDesc(userId: UUID): List<MealType>
    fun existsByUser_IdAndNameIgnoreCase(userId: UUID, name: String): Boolean
}

