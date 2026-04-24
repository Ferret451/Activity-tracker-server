package com.bsuir.Activity_tracker_server.food

import com.bsuir.Activity_tracker_server.food.client.FoodDataClient
import com.bsuir.Activity_tracker_server.food.client.UsdaFoodItemResponse
import com.bsuir.Activity_tracker_server.food.client.UsdaFoodNutrient
import com.bsuir.Activity_tracker_server.food.client.UsdaSearchResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FoodServiceTest {

    @Test
    fun `search maps USDA nutrients into calories and macros`() {
        val client = object : FoodDataClient {
            override fun searchFoods(
                query: String,
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
                sortOrder: String?,
                brandOwner: String?,
            ): UsdaSearchResponse =
                UsdaSearchResponse(
                    totalHits = 1,
                    currentPage = 1,
                    totalPages = 1,
                    foods = listOf(
                        UsdaFoodItemResponse(
                            fdcId = 12345,
                            description = "Sample Food",
                            foodNutrients = listOf(
                                UsdaFoodNutrient(number = "1008", amount = 200.0),
                                UsdaFoodNutrient(nutrientName = "Protein", value = 10.0),
                                UsdaFoodNutrient(number = "1004", amount = 7.5),
                                UsdaFoodNutrient(nutrientNumber = "1005", amount = 22.3),
                            ),
                        ),
                    ),
                )

            override fun getFoodById(fdcId: Long): UsdaFoodItemResponse =
                throw UnsupportedOperationException()

            override fun getFoodsByIds(fdcIds: List<Long>): List<UsdaFoodItemResponse> =
                throw UnsupportedOperationException()
        }

        val service = FoodService(client)
        val result = service.search("sample", 1, 25, emptyList(), null, null, null)

        assertEquals(1, result.foods.size)
        assertEquals(200.0, result.foods.first().nutrients.caloriesKcal)
        assertEquals(10.0, result.foods.first().nutrients.proteinG)
        assertEquals(7.5, result.foods.first().nutrients.fatG)
        assertEquals(22.3, result.foods.first().nutrients.carbsG)
    }
}

