package com.bsuir.Activity_tracker_server.nutrition

import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface MealTypeRepository : JpaRepository<MealType, UUID> {
    fun findAllByOrderByNameAsc(): List<MealType>

    fun existsByNameIgnoreCase(name: String): Boolean

    fun existsByNameIgnoreCaseAndIdNot(name: String, id: UUID): Boolean
}
