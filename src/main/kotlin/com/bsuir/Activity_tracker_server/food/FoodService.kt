package com.bsuir.Activity_tracker_server.food

import com.bsuir.Activity_tracker_server.food.client.FoodDataClient
import com.bsuir.Activity_tracker_server.food.client.UsdaFoodItemResponse
import com.bsuir.Activity_tracker_server.food.client.UsdaFoodNutrient
import com.bsuir.Activity_tracker_server.food.client.UsdaSearchResponse
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
        ).map(::toSummaryResponse)

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
        ).map(::toSummaryResponse)

    fun getById(
        fdcId: Long,
        format: String?,
        nutrients: List<Long>,
    ): FoodDetailsResponse =
        toDetailsResponse(
            foodDataClient.getFoodById(
                fdcId = fdcId,
                format = normalizeFormat(format),
                nutrients = nutrients.distinct(),
            ),
        )

    fun lookupByIdsGet(
        fdcIds: List<Long>,
        format: String?,
        nutrients: List<Long>,
    ): List<FoodDetailsResponse> =
        foodDataClient.getFoodsByIdsGet(
            fdcIds = fdcIds.distinct(),
            format = normalizeFormat(format),
            nutrients = nutrients.distinct(),
        ).map(::toDetailsResponse)

    fun lookupByIdsPost(
        fdcIds: List<Long>,
        format: String?,
        nutrients: List<Long>,
    ): List<FoodDetailsResponse> =
        foodDataClient.getFoodsByIdsPost(
            fdcIds = fdcIds.distinct(),
            format = normalizeFormat(format),
            nutrients = nutrients.distinct(),
        ).map(::toDetailsResponse)

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
            foods = response.foods.map(::toSummaryResponse),
        )
    }

    private fun normalizeFormat(format: String?): String? =
        format?.trim()?.lowercase()?.takeIf { it == "abridged" || it == "full" }

    private fun toSummaryResponse(food: UsdaFoodItemResponse): FoodSummaryResponse =
        FoodSummaryResponse(
            fdcId = food.fdcId,
            description = food.description.orEmpty(),
            dataType = food.dataType,
            brandOwner = food.brandOwner,
            servingSize = food.servingSize,
            servingSizeUnit = food.servingSizeUnit,
            nutrients = extractNutrients(food.foodNutrients),
        )

    private fun toDetailsResponse(food: UsdaFoodItemResponse): FoodDetailsResponse =
        FoodDetailsResponse(
            fdcId = food.fdcId,
            description = food.description.orEmpty(),
            dataType = food.dataType,
            brandOwner = food.brandOwner,
            ingredients = food.ingredients,
            servingSize = food.servingSize,
            servingSizeUnit = food.servingSizeUnit,
            nutrients = extractNutrients(food.foodNutrients),
            portions = food.foodPortions.map {
                FoodPortionResponse(
                    amount = it.amount,
                    gramWeight = it.gramWeight,
                    description = it.portionDescription ?: it.modifier ?: it.measureUnit?.name,
                )
            },
        )

    private fun extractNutrients(nutrients: List<UsdaFoodNutrient>): FoodNutrientsResponse {
        val normalized = nutrients.mapNotNull(::toNutrientValue)

        return FoodNutrientsResponse(
            caloriesKcal = normalized.findValue(
                numberCodes = setOf("1008", "208"),
                nameMatcher = { name ->
                    name.contains("energy") || name.contains("calories") || name.contains("kcal")
                },
            ),
            proteinG = normalized.findValue(
                numberCodes = setOf("1003", "203"),
                nameMatcher = { name -> name.contains("protein") },
            ),
            fatG = normalized.findValue(
                numberCodes = setOf("1004", "204"),
                nameMatcher = { name -> name.contains("fat") || name.contains("lipid") },
            ),
            carbsG = normalized.findValue(
                numberCodes = setOf("1005", "205"),
                nameMatcher = { name -> name.contains("carbohydrate") || name.contains("carbs") },
            ),
        )
    }

    private fun toNutrientValue(nutrient: UsdaFoodNutrient): NutrientValue? {
        val amount = nutrient.amount ?: nutrient.value ?: return null
        val number = nutrient.nutrient?.number ?: nutrient.nutrientNumber ?: nutrient.number
        val name = nutrient.nutrient?.name ?: nutrient.nutrientName ?: nutrient.name

        return NutrientValue(
            number = number?.trim()?.lowercase(),
            name = name?.trim()?.lowercase(),
            amount = amount,
        )
    }

    private fun List<NutrientValue>.findValue(
        numberCodes: Set<String>,
        nameMatcher: (String) -> Boolean,
    ): Double? {
        val byCode = firstOrNull { value ->
            value.number != null && value.number in numberCodes
        }
        if (byCode != null) {
            return byCode.amount
        }

        return firstOrNull { value ->
            value.name != null && nameMatcher(value.name)
        }?.amount
    }

    private data class NutrientValue(
        val number: String?,
        val name: String?,
        val amount: Double,
    )
}
