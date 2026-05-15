package com.bsuir.Activity_tracker_server.nutrition

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface NutritionRecordNutrientRepository : JpaRepository<NutritionRecordNutrient, UUID> {
    @Modifying
    @Query("DELETE FROM NutritionRecordNutrient n WHERE n.nutritionRecord.id = :recordId")
    fun deleteByNutritionRecordId(@Param("recordId") recordId: UUID)
}
