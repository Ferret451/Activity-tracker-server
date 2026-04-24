package com.bsuir.Activity_tracker_server.food.client

interface FoodDataClient {
    fun searchFoods(
        query: String,
        pageNumber: Int,
        pageSize: Int,
        dataTypes: List<String>,
        sortBy: String?,
        sortOrder: String?,
        brandOwner: String?,
    ): UsdaSearchResponse

    fun getFoodById(fdcId: Long): UsdaFoodItemResponse

    fun getFoodsByIds(fdcIds: List<Long>): List<UsdaFoodItemResponse>
}

