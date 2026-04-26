package com.bsuir.Activity_tracker_server.food.client

interface FoodDataClient {
    fun searchFoodsGet(
        query: String,
        pageNumber: Int,
        pageSize: Int,
        dataTypes: List<String>,
        sortBy: String?,
        sortOrder: String?,
        brandOwner: String?,
    ): UsdaSearchResponse

    fun searchFoodsPost(
        query: String,
        pageNumber: Int,
        pageSize: Int,
        dataTypes: List<String>,
        sortBy: String?,
        sortOrder: String?,
        brandOwner: String?,
    ): UsdaSearchResponse

    fun listFoodsGet(
        pageNumber: Int,
        pageSize: Int,
        dataTypes: List<String>,
        sortBy: String?,
    ): List<UsdaFoodItemResponse>

    fun listFoodsPost(
        pageNumber: Int,
        pageSize: Int,
        dataTypes: List<String>,
        sortBy: String?,
    ): List<UsdaFoodItemResponse>

    fun getFoodById(
        fdcId: Long,
        format: String? = null,
        nutrients: List<Long> = emptyList(),
    ): UsdaFoodItemResponse

    fun getFoodsByIdsGet(
        fdcIds: List<Long>,
        format: String? = null,
        nutrients: List<Long> = emptyList(),
    ): List<UsdaFoodItemResponse>

    fun getFoodsByIdsPost(
        fdcIds: List<Long>,
        format: String? = null,
        nutrients: List<Long> = emptyList(),
    ): List<UsdaFoodItemResponse>
}

