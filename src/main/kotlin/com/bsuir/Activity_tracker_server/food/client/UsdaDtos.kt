package com.bsuir.Activity_tracker_server.food.client

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class UsdaSearchResponse(
    val totalHits: Int = 0,
    val currentPage: Int = 1,
    val totalPages: Int = 0,
    val foods: List<UsdaFoodItemResponse> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UsdaFoodItemResponse(
    val fdcId: Long,
    val description: String? = null,
    val dataType: String? = null,
    val brandOwner: String? = null,
    val ingredients: String? = null,
    val servingSize: Double? = null,
    val servingSizeUnit: String? = null,
    val foodNutrients: List<UsdaFoodNutrient> = emptyList(),
    val foodPortions: List<UsdaFoodPortion> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UsdaFoodNutrient(
    val amount: Double? = null,
    val value: Double? = null,
    val unitName: String? = null,
    val name: String? = null,
    val number: String? = null,
    val nutrientName: String? = null,
    val nutrientNumber: String? = null,
    val nutrient: UsdaNutrientDescriptor? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UsdaNutrientDescriptor(
    val id: Long? = null,
    val number: String? = null,
    val name: String? = null,
    val unitName: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UsdaFoodPortion(
    val amount: Double? = null,
    val gramWeight: Double? = null,
    val modifier: String? = null,
    val portionDescription: String? = null,
    val measureUnit: UsdaMeasureUnit? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UsdaMeasureUnit(
    val name: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UsdaSearchRequest(
    val query: String,
    val dataType: List<String> = emptyList(),
    val pageSize: Int,
    val pageNumber: Int,
    val sortBy: String? = null,
    val sortOrder: String? = null,
    val brandOwner: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UsdaFoodsRequest(
    val fdcIds: List<Long>,
    val format: String? = null,
    val nutrients: List<Long> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class UsdaFoodsListRequest(
    val dataType: List<String> = emptyList(),
    val pageSize: Int,
    val pageNumber: Int,
    val sortBy: String? = null,
)

