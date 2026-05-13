package com.bsuir.Activity_tracker_server.food

import com.bsuir.Activity_tracker_server.food.client.FoodDataClient
import com.bsuir.Activity_tracker_server.food.client.UsdaFoodItemResponse
import com.bsuir.Activity_tracker_server.food.client.UsdaSearchResponse
import com.bsuir.Activity_tracker_server.food.exception.FoodNotFoundException
import org.springframework.stereotype.Service

@Service
class FoodService(
    private val foodDataClient: FoodDataClient,
) {

    fun searchGet(
        query: String,
        pageNumber: Int,
        pageSize: Int,
        dataTypes: List<String>,
        sortBy: String?,
        sortOrder: String?,
        brandOwner: String?,
    ): FoodSearchResponse {
        val response = foodDataClient.searchFoodsGet(
            query = query.trim(),
            pageNumber = pageNumber,
            pageSize = pageSize,
            dataTypes = dataTypes.map(String::trim).filter(String::isNotBlank),
            sortBy = sortBy?.trim()?.takeIf(String::isNotBlank),
            sortOrder = sortOrder?.trim()?.lowercase()?.takeIf(String::isNotBlank),
            brandOwner = brandOwner?.trim()?.takeIf(String::isNotBlank),
        )

        return toSearchResponse(response)
    }

    fun searchPost(
        query: String,
        pageNumber: Int,
        pageSize: Int,
        dataTypes: List<String>,
        sortBy: String?,
        sortOrder: String?,
        brandOwner: String?,
    ): FoodSearchResponse {
        val response = foodDataClient.searchFoodsPost(
            query = query.trim(),
            pageNumber = pageNumber,
            pageSize = pageSize,
            dataTypes = dataTypes.map(String::trim).filter(String::isNotBlank),
            sortBy = sortBy?.trim()?.takeIf(String::isNotBlank),
            sortOrder = sortOrder?.trim()?.lowercase()?.takeIf(String::isNotBlank),
            brandOwner = brandOwner?.trim()?.takeIf(String::isNotBlank),
        )

        return toSearchResponse(response)
    }

    fun listGet(
        pageNumber: Int,
        pageSize: Int,
        dataTypes: List<String>,
        sortBy: String?,
    ): List<FoodSummaryResponse> =
        foodDataClient.listFoodsGet(
            pageNumber = pageNumber,
            pageSize = pageSize,
            dataTypes = dataTypes.map(String::trim).filter(String::isNotBlank),
            sortBy = sortBy?.trim()?.takeIf(String::isNotBlank),
        ).mapNotNull(::toSummaryResponseOrNull)

    fun listPost(
        pageNumber: Int,
        pageSize: Int,
        dataTypes: List<String>,
        sortBy: String?,
    ): List<FoodSummaryResponse> =
        foodDataClient.listFoodsPost(
            pageNumber = pageNumber,
            pageSize = pageSize,
            dataTypes = dataTypes.map(String::trim).filter(String::isNotBlank),
            sortBy = sortBy?.trim()?.takeIf(String::isNotBlank),
        ).mapNotNull(::toSummaryResponseOrNull)

    fun getById(
        fdcId: Long,
        format: String?,
        nutrients: List<Long>,
    ): FoodDetailsResponse =
        toDetailsResponseOrNull(
            foodDataClient.getFoodById(
                fdcId = fdcId,
                format = normalizeFormat(format),
                nutrients = nutrients.distinct(),
            ),
        ) ?: throw FoodNotFoundException(fdcId)

    fun lookupByIdsGet(
        fdcIds: List<Long>,
        format: String?,
        nutrients: List<Long>,
    ): List<FoodDetailsResponse> =
        foodDataClient.getFoodsByIdsGet(
            fdcIds = fdcIds.distinct(),
            format = normalizeFormat(format),
            nutrients = nutrients.distinct(),
        ).mapNotNull(::toDetailsResponseOrNull)

    fun lookupByIdsPost(
        fdcIds: List<Long>,
        format: String?,
        nutrients: List<Long>,
    ): List<FoodDetailsResponse> =
        foodDataClient.getFoodsByIdsPost(
            fdcIds = fdcIds.distinct(),
            format = normalizeFormat(format),
            nutrients = nutrients.distinct(),
        ).mapNotNull(::toDetailsResponseOrNull)

    @Deprecated("Use searchGet")
    fun search(
        query: String,
        pageNumber: Int,
        pageSize: Int,
        dataTypes: List<String>,
        sortBy: String?,
        sortOrder: String?,
        brandOwner: String?,
    ): FoodSearchResponse = searchGet(query, pageNumber, pageSize, dataTypes, sortBy, sortOrder, brandOwner)

    @Deprecated("Use getById with format/nutrients")
    fun getById(fdcId: Long): FoodDetailsResponse =
        getById(fdcId, null, emptyList())

    @Deprecated("Use lookupByIdsPost")
    fun lookupByIds(fdcIds: List<Long>): List<FoodDetailsResponse> =
        lookupByIdsPost(fdcIds, null, emptyList())

    private fun toSearchResponse(response: UsdaSearchResponse): FoodSearchResponse {
        return FoodSearchResponse(
            totalHits = response.totalHits,
            currentPage = response.currentPage,
            totalPages = response.totalPages,
            foods = response.foods.mapNotNull(::toSummaryResponseOrNull),
        )
    }

    private fun normalizeFormat(format: String?): String? =
        format?.trim()?.lowercase()?.takeIf { it == "abridged" || it == "full" }

    private fun toSummaryResponseOrNull(food: UsdaFoodItemResponse): FoodSummaryResponse? {
        val nutrients = UsdaNutrientsParser.extractNutrients(food.foodNutrients)
        if (!UsdaNutrientsParser.hasRequiredMacros(nutrients)) {
            return null
        }
        if (!UsdaNutrientsParser.caloriesAcceptableForSearchList(nutrients)) {
            return null
        }

        return FoodSummaryResponse(
            fdcId = food.fdcId,
            description = food.description.orEmpty(),
            dataType = food.dataType,
            brandOwner = food.brandOwner,
            servingSize = food.servingSize,
            servingSizeUnit = food.servingSizeUnit,
            nutrients = nutrients,
        )
    }

    private fun toDetailsResponseOrNull(food: UsdaFoodItemResponse): FoodDetailsResponse? {
        val nutrients = UsdaNutrientsParser.extractNutrients(food.foodNutrients)
        if (!UsdaNutrientsParser.hasRequiredMacros(nutrients)) {
            return null
        }

        return FoodDetailsResponse(
            fdcId = food.fdcId,
            description = food.description.orEmpty(),
            dataType = food.dataType,
            brandOwner = food.brandOwner,
            ingredients = food.ingredients,
            servingSize = food.servingSize,
            servingSizeUnit = food.servingSizeUnit,
            nutrients = nutrients,
            portions = food.foodPortions.map {
                FoodPortionResponse(
                    amount = it.amount,
                    gramWeight = it.gramWeight,
                    description = it.portionDescription ?: it.modifier ?: it.measureUnit?.name,
                )
            },
        )
    }

}
