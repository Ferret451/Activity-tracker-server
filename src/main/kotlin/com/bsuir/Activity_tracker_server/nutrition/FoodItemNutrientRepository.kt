package com.bsuir.Activity_tracker_server.nutrition

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface FoodItemNutrientRepository : JpaRepository<FoodItemNutrient, UUID> {
    @Modifying
    @Query("DELETE FROM FoodItemNutrient n WHERE n.foodItem.id = :foodItemId")
    fun deleteByFoodItemId(@Param("foodItemId") foodItemId: UUID)
}
