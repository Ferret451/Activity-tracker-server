package com.bsuir.Activity_tracker_server.userdata

import com.bsuir.Activity_tracker_server.common.AccessPolicy
import com.bsuir.Activity_tracker_server.nutrition.CreateFoodItemRequest
import com.bsuir.Activity_tracker_server.nutrition.CreateNutritionRecordRequest
import com.bsuir.Activity_tracker_server.nutrition.NutritionQueryFilter
import com.bsuir.Activity_tracker_server.nutrition.NutritionService
import com.bsuir.Activity_tracker_server.nutrition.UpdateFoodItemRequest
import com.bsuir.Activity_tracker_server.nutrition.UpdateNutritionRecordRequest
import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.security.core.Authentication
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.util.UUID

@RestController
@RequestMapping("/api/users/{userId}/nutrition")
@Validated
class UserNutritionController(
    private val nutritionService: NutritionService,
    private val accessPolicy: AccessPolicy,
) {

    @PostMapping("/food-items")
    @ResponseStatus(HttpStatus.CREATED)
    fun createFoodItem(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @Valid @RequestBody request: CreateFoodItemRequest,
    ) = doWithAccess(authentication, userId) { nutritionService.createFoodItem(userId, request) }

    @PostMapping("/food-items/import/usda/{fdcId}")
    fun importFoodItemFromUsda(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable @Positive fdcId: Long,
    ) = doWithAccess(authentication, userId) { nutritionService.importFoodItemFromUsda(userId, fdcId) }

    @GetMapping("/food-items")
    fun getFoodItems(
        @PathVariable userId: UUID,
        authentication: Authentication,
    ) = doWithAccess(authentication, userId) { nutritionService.getFoodItems(userId) }

    @GetMapping("/food-items/{foodItemId}")
    fun getFoodItem(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable foodItemId: UUID,
    ) = doWithAccess(authentication, userId) { nutritionService.getFoodItem(userId, foodItemId) }

    @PutMapping("/food-items/{foodItemId}")
    fun updateFoodItem(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable foodItemId: UUID,
        @Valid @RequestBody request: UpdateFoodItemRequest,
    ) = doWithAccess(authentication, userId) { nutritionService.updateFoodItem(userId, foodItemId, request) }

    @DeleteMapping("/food-items/{foodItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteFoodItem(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable foodItemId: UUID,
    ) {
        doWithAccess(authentication, userId) { nutritionService.deleteFoodItem(userId, foodItemId) }
    }

    @PostMapping("/records")
    @ResponseStatus(HttpStatus.CREATED)
    fun createRecord(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @Valid @RequestBody request: CreateNutritionRecordRequest,
    ) = doWithAccess(authentication, userId) { nutritionService.createRecord(userId, request) }

    @GetMapping("/records")
    fun getRecords(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(required = false) mealTypeId: UUID?,
        @RequestParam(required = false) foodItemId: UUID?,
    ) = doWithAccess(authentication, userId) {
        nutritionService.getRecords(
            userId,
            NutritionQueryFilter(from = from, to = to, mealTypeId = mealTypeId, foodItemId = foodItemId),
        )
    }

    @GetMapping("/records/{recordId}")
    fun getRecord(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable recordId: UUID,
    ) = doWithAccess(authentication, userId) { nutritionService.getRecord(userId, recordId) }

    @PutMapping("/records/{recordId}")
    fun updateRecord(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable recordId: UUID,
        @Valid @RequestBody request: UpdateNutritionRecordRequest,
    ) = doWithAccess(authentication, userId) { nutritionService.updateRecord(userId, recordId, request) }

    @DeleteMapping("/records/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteRecord(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @PathVariable recordId: UUID,
    ) {
        doWithAccess(authentication, userId) { nutritionService.deleteRecord(userId, recordId) }
    }

    @GetMapping("/history")
    fun getHistory(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
    ) = doWithAccess(authentication, userId) { nutritionService.getHistory(userId, from, to) }

    @GetMapping("/summary")
    fun getSummary(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(required = false) mealTypeId: UUID?,
        @RequestParam(required = false) foodItemId: UUID?,
    ) = doWithAccess(authentication, userId) {
        nutritionService.getSummary(
            userId,
            NutritionQueryFilter(from = from, to = to, mealTypeId = mealTypeId, foodItemId = foodItemId),
        )
    }

    @GetMapping("/daily")
    fun getDaily(
        @PathVariable userId: UUID,
        authentication: Authentication,
        @RequestParam(required = false) date: LocalDate?,
        @RequestParam(required = false) zone: String?,
    ) = doWithAccess(authentication, userId) {
        nutritionService.getDaily(userId, date, zone.orEmpty())
    }

    private inline fun <T> doWithAccess(authentication: Authentication, userId: UUID, block: () -> T): T {
        accessPolicy.requireSelfOrAdmin(authentication, userId)
        return block()
    }
}
