package com.bsuir.Activity_tracker_server.nutrition

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.UUID

interface NutritionRecordRepository : JpaRepository<NutritionRecord, UUID> {
    fun findByIdAndUser_Id(id: UUID, userId: UUID): NutritionRecord?
    fun findAllByUser_IdOrderByConsumedAtDesc(userId: UUID): List<NutritionRecord>
    fun existsByMealType_IdAndUser_Id(mealTypeId: UUID, userId: UUID): Boolean
    fun existsByFoodItem_IdAndUser_Id(foodItemId: UUID, userId: UUID): Boolean

    @Query(
        """
        select nr
        from NutritionRecord nr
        where nr.user.id = :userId
          and (:fromDateTime is null or nr.consumedAt >= :fromDateTime)
          and (:toDateTimeExclusive is null or nr.consumedAt < :toDateTimeExclusive)
          and (:mealTypeId is null or nr.mealType.id = :mealTypeId)
          and (:foodItemId is null or nr.foodItem.id = :foodItemId)
        order by nr.consumedAt desc
        """,
    )
    fun findFiltered(
        @Param("userId") userId: UUID,
        @Param("fromDateTime") fromDateTime: OffsetDateTime?,
        @Param("toDateTimeExclusive") toDateTimeExclusive: OffsetDateTime?,
        @Param("mealTypeId") mealTypeId: UUID?,
        @Param("foodItemId") foodItemId: UUID?,
    ): List<NutritionRecord>
}

