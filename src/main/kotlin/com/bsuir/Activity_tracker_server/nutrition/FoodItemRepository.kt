package com.bsuir.Activity_tracker_server.nutrition

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FoodItemRepository : JpaRepository<FoodItem, UUID> {
    fun findByIdAndUser_Id(id: UUID, userId: UUID): FoodItem?
    fun findByUser_IdAndFdcId(userId: UUID, fdcId: Long): FoodItem?
    fun findAllByUser_IdOrderByCreatedAtDesc(userId: UUID): List<FoodItem>
    fun existsByUser_IdAndNameIgnoreCase(userId: UUID, name: String): Boolean
}

