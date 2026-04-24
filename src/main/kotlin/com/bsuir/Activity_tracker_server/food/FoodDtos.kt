package com.bsuir.Activity_tracker_server.food

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size

data class FoodLookupRequest(
    @field:Size(min = 1, max = 50)
    val fdcIds: List<@Positive Long>,
)

data class FoodSearchResponse(
    val totalHits: Int,
    val currentPage: Int,
    val totalPages: Int,
    val foods: List<FoodSummaryResponse>,
)

data class FoodSummaryResponse(
    val fdcId: Long,
    val description: String,
    val dataType: String?,
    val brandOwner: String?,
    val servingSize: Double?,
    val servingSizeUnit: String?,
    val nutrients: FoodNutrientsResponse,
)

data class FoodDetailsResponse(
    val fdcId: Long,
    val description: String,
    val dataType: String?,
    val brandOwner: String?,
    val ingredients: String?,
    val servingSize: Double?,
    val servingSizeUnit: String?,
    val nutrients: FoodNutrientsResponse,
    val portions: List<FoodPortionResponse>,
)

data class FoodNutrientsResponse(
    val caloriesKcal: Double?,
    val proteinG: Double?,
    val fatG: Double?,
    val carbsG: Double?,
)

data class FoodPortionResponse(
    val amount: Double?,
    val gramWeight: Double?,
    val description: String?,
)

