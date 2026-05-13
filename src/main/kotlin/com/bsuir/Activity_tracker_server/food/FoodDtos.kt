package com.bsuir.Activity_tracker_server.food

import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Max

data class FoodLookupRequest(
    @field:Size(min = 1, max = 50)
    val fdcIds: List<@Positive Long>,

    @field:Size(max = 16)
    @field:jakarta.validation.constraints.Pattern(regexp = "(?i)abridged|full")
    val format: String? = null,

    @field:Size(max = 100)
    val nutrients: List<@Positive Long>? = null,
)

data class FoodSearchRequest(
    @field:Size(min = 1, max = 200)
    val query: String,
    val dataType: List<String>? = null,

    @field:Min(1)
    @field:Max(200)
    val pageSize: Int? = null,

    @field:Min(1)
    val pageNumber: Int? = null,
    val sortBy: String? = null,

    @field:jakarta.validation.constraints.Pattern(regexp = "(?i)asc|desc")
    val sortOrder: String? = null,
    val brandOwner: String? = null,
)

data class FoodListRequest(
    val dataType: List<String>? = null,

    @field:Min(1)
    @field:Max(200)
    val pageSize: Int? = null,

    @field:Min(1)
    val pageNumber: Int? = null,
    val sortBy: String? = null,
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
    val additionalNutrients: List<FoodNutrientEntry> = emptyList(),
)

data class FoodNutrientEntry(
    val name: String,
    val amount: Double,
    val unitName: String? = null,
    val number: String? = null,
)

data class FoodPortionResponse(
    val amount: Double?,
    val gramWeight: Double?,
    val description: String?,
)

