package com.bsuir.Activity_tracker_server.nutrition

import jakarta.validation.Valid
import jakarta.validation.constraints.Positive
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
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
@RequestMapping("/api/me/nutrition")
@Validated
class NutritionController(
    private val nutritionService: NutritionService,
) {

    @PostMapping("/meal-types")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    fun createMealType(
        @Valid @RequestBody request: CreateMealTypeRequest,
    ): MealTypeResponse = nutritionService.createMealType(request)

    @GetMapping("/meal-types")
    fun getMealTypes(): List<MealTypeResponse> =
        nutritionService.getMealTypes()

    @PutMapping("/meal-types/{mealTypeId}")
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    fun updateMealType(
        @PathVariable mealTypeId: UUID,
        @Valid @RequestBody request: UpdateMealTypeRequest,
    ): MealTypeResponse = nutritionService.updateMealType(mealTypeId, request)

    @DeleteMapping("/meal-types/{mealTypeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('SCOPE_ADMIN')")
    fun deleteMealType(
        @PathVariable mealTypeId: UUID,
    ) {
        nutritionService.deleteMealType(mealTypeId)
    }

    @PostMapping("/food-items")
    @ResponseStatus(HttpStatus.CREATED)
    fun createFoodItem(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreateFoodItemRequest,
    ): FoodItemResponse = nutritionService.createFoodItem(extractUserId(jwt), request)

    @PostMapping("/food-items/import/usda/{fdcId}")
    fun importFoodItemFromUsda(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable @Positive fdcId: Long,
    ): FoodItemResponse = nutritionService.importFoodItemFromUsda(extractUserId(jwt), fdcId)

    @GetMapping("/food-items")
    fun getFoodItems(@AuthenticationPrincipal jwt: Jwt): List<FoodItemResponse> =
        nutritionService.getFoodItems(extractUserId(jwt))

    @GetMapping("/food-items/{foodItemId}")
    fun getFoodItem(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable foodItemId: UUID,
    ): FoodItemResponse = nutritionService.getFoodItem(extractUserId(jwt), foodItemId)

    @PutMapping("/food-items/{foodItemId}")
    fun updateFoodItem(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable foodItemId: UUID,
        @Valid @RequestBody request: UpdateFoodItemRequest,
    ): FoodItemResponse = nutritionService.updateFoodItem(extractUserId(jwt), foodItemId, request)

    @DeleteMapping("/food-items/{foodItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteFoodItem(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable foodItemId: UUID,
    ) {
        nutritionService.deleteFoodItem(extractUserId(jwt), foodItemId)
    }

    @PostMapping("/records")
    @ResponseStatus(HttpStatus.CREATED)
    fun createRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @Valid @RequestBody request: CreateNutritionRecordRequest,
    ): NutritionRecordResponse = nutritionService.createRecord(extractUserId(jwt), request)

    @GetMapping("/records")
    fun getRecords(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(required = false) mealTypeId: UUID?,
        @RequestParam(required = false) foodItemId: UUID?,
    ): List<NutritionRecordResponse> =
        nutritionService.getRecords(
            userId = extractUserId(jwt),
            filter = NutritionQueryFilter(
                from = from,
                to = to,
                mealTypeId = mealTypeId,
                foodItemId = foodItemId,
            ),
        )

    @GetMapping("/records/{recordId}")
    fun getRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable recordId: UUID,
    ): NutritionRecordResponse = nutritionService.getRecord(extractUserId(jwt), recordId)

    @PutMapping("/records/{recordId}")
    fun updateRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable recordId: UUID,
        @Valid @RequestBody request: UpdateNutritionRecordRequest,
    ): NutritionRecordResponse = nutritionService.updateRecord(extractUserId(jwt), recordId, request)

    @DeleteMapping("/records/{recordId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteRecord(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable recordId: UUID,
    ) {
        nutritionService.deleteRecord(extractUserId(jwt), recordId)
    }

    @GetMapping("/history")
    fun getHistory(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
    ): List<NutritionHistoryDayResponse> =
        nutritionService.getHistory(
            userId = extractUserId(jwt),
            from = from,
            to = to,
        )

    @GetMapping("/summary")
    fun getSummary(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) from: LocalDate?,
        @RequestParam(required = false) to: LocalDate?,
        @RequestParam(required = false) mealTypeId: UUID?,
        @RequestParam(required = false) foodItemId: UUID?,
    ): NutritionSummaryResponse =
        nutritionService.getSummary(
            userId = extractUserId(jwt),
            filter = NutritionQueryFilter(
                from = from,
                to = to,
                mealTypeId = mealTypeId,
                foodItemId = foodItemId,
            ),
        )

    @GetMapping("/daily")
    fun getDaily(
        @AuthenticationPrincipal jwt: Jwt,
        @RequestParam(required = false) date: LocalDate?,
        @RequestParam(required = false) zone: String?,
    ): NutritionDailyResponse =
        nutritionService.getDaily(
            userId = extractUserId(jwt),
            date = date,
            zoneId = zone.orEmpty(),
        )

    private fun extractUserId(jwt: Jwt): UUID = UUID.fromString(jwt.subject)
}

