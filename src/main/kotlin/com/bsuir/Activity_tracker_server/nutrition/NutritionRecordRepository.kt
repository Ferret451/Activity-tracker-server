package com.bsuir.Activity_tracker_server.nutrition

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.UUID

interface NutritionRecordRepository :
    JpaRepository<NutritionRecord, UUID>,
    JpaSpecificationExecutor<NutritionRecord> {
    fun findByIdAndUser_Id(id: UUID, userId: UUID): NutritionRecord?
    fun findAllByUser_IdOrderByConsumedAtDesc(userId: UUID): List<NutritionRecord>
    fun existsByMealType_IdAndUser_Id(mealTypeId: UUID, userId: UUID): Boolean

    fun existsByMealType_Id(mealTypeId: UUID): Boolean
    fun existsByFoodItem_IdAndUser_Id(foodItemId: UUID, userId: UUID): Boolean
}
