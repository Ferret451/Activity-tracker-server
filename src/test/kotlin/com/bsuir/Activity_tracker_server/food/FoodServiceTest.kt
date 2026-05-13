package com.bsuir.Activity_tracker_server.food

import com.bsuir.Activity_tracker_server.food.client.FoodDataClient
import com.bsuir.Activity_tracker_server.food.client.UsdaFoodItemResponse
import com.bsuir.Activity_tracker_server.food.client.UsdaFoodNutrient
import com.bsuir.Activity_tracker_server.food.client.UsdaSearchResponse
import com.bsuir.Activity_tracker_server.food.exception.FoodNotFoundException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class FoodServiceTest {

    @Test
    fun `search maps USDA nutrients into calories and macros`() {
        val client = object : FoodDataClient {
            override fun searchFoodsGet(
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

            override fun searchFoodsPost(
                query: String,
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
                sortOrder: String?,
                brandOwner: String?,
            ): UsdaSearchResponse = throw UnsupportedOperationException()

            override fun listFoodsGet(
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()

            override fun listFoodsPost(
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()

            override fun getFoodById(
                fdcId: Long,
                format: String?,
                nutrients: List<Long>,
            ): UsdaFoodItemResponse = throw UnsupportedOperationException()

            override fun getFoodsByIdsGet(
                fdcIds: List<Long>,
                format: String?,
                nutrients: List<Long>,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()

            override fun getFoodsByIdsPost(
                fdcIds: List<Long>,
                format: String?,
                nutrients: List<Long>,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()
        }

        val service = FoodService(client)
        val result = service.searchGet("sample", 1, 25, emptyList(), null, null, null)

        assertEquals(1, result.foods.size)
        assertEquals(200.0, result.foods.first().nutrients.caloriesKcal)
        assertEquals(10.0, result.foods.first().nutrients.proteinG)
        assertEquals(7.5, result.foods.first().nutrients.fatG)
        assertEquals(22.3, result.foods.first().nutrients.carbsG)
        assertTrue(result.foods.first().nutrients.additionalNutrients.isEmpty())
    }

    @Test
    fun `search returns additional nutrients that are non zero`() {
        val client = object : FoodDataClient {
            override fun searchFoodsGet(
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
                            fdcId = 123,
                            description = "With additional nutrients",
                            foodNutrients = listOf(
                                UsdaFoodNutrient(number = "1008", amount = 10.0),
                                UsdaFoodNutrient(number = "1003", amount = 1.0),
                                UsdaFoodNutrient(number = "1004", amount = 2.0),
                                UsdaFoodNutrient(number = "1005", amount = 3.0),
                                UsdaFoodNutrient(name = "Fiber, total dietary", unitName = "G", amount = 4.0),
                                UsdaFoodNutrient(name = "Sodium, Na", unitName = "MG", amount = 0.0),
                            ),
                        ),
                    ),
                )

            override fun searchFoodsPost(
                query: String,
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
                sortOrder: String?,
                brandOwner: String?,
            ): UsdaSearchResponse = throw UnsupportedOperationException()

            override fun listFoodsGet(
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()

            override fun listFoodsPost(
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()

            override fun getFoodById(
                fdcId: Long,
                format: String?,
                nutrients: List<Long>,
            ): UsdaFoodItemResponse = throw UnsupportedOperationException()

            override fun getFoodsByIdsGet(
                fdcIds: List<Long>,
                format: String?,
                nutrients: List<Long>,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()

            override fun getFoodsByIdsPost(
                fdcIds: List<Long>,
                format: String?,
                nutrients: List<Long>,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()
        }

        val service = FoodService(client)
        val result = service.searchGet("x", 1, 25, emptyList(), null, null, null)

        assertEquals(1, result.foods.size)
        val additional = result.foods.first().nutrients.additionalNutrients
        assertEquals(1, additional.size)
        assertEquals("fiber, total dietary", additional.first().name)
        assertEquals(4.0, additional.first().amount)
        assertEquals("G", additional.first().unitName)
    }

    @Test
    fun `search filters out foods without full macros`() {
        val client = object : FoodDataClient {
            override fun searchFoodsGet(
                query: String,
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
                sortOrder: String?,
                brandOwner: String?,
            ): UsdaSearchResponse =
                UsdaSearchResponse(
                    totalHits = 2,
                    currentPage = 1,
                    totalPages = 1,
                    foods = listOf(
                        UsdaFoodItemResponse(
                            fdcId = 1,
                            description = "Without protein",
                            foodNutrients = listOf(
                                UsdaFoodNutrient(number = "1004", amount = 1.0),
                                UsdaFoodNutrient(number = "1005", amount = 2.0),
                            ),
                        ),
                        UsdaFoodItemResponse(
                            fdcId = 2,
                            description = "With macros",
                            foodNutrients = listOf(
                                UsdaFoodNutrient(number = "1008", amount = 100.0),
                                UsdaFoodNutrient(number = "1003", amount = 3.0),
                                UsdaFoodNutrient(number = "1004", amount = 4.0),
                                UsdaFoodNutrient(number = "1005", amount = 5.0),
                            ),
                        ),
                    ),
                )

            override fun searchFoodsPost(
                query: String,
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
                sortOrder: String?,
                brandOwner: String?,
            ): UsdaSearchResponse = throw UnsupportedOperationException()

            override fun listFoodsGet(
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()

            override fun listFoodsPost(
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()

            override fun getFoodById(
                fdcId: Long,
                format: String?,
                nutrients: List<Long>,
            ): UsdaFoodItemResponse = throw UnsupportedOperationException()

            override fun getFoodsByIdsGet(
                fdcIds: List<Long>,
                format: String?,
                nutrients: List<Long>,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()

            override fun getFoodsByIdsPost(
                fdcIds: List<Long>,
                format: String?,
                nutrients: List<Long>,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()
        }

        val service = FoodService(client)
        val result = service.searchGet("sample", 1, 25, emptyList(), null, null, null)

        assertEquals(1, result.foods.size)
        assertEquals(2, result.foods.first().fdcId)
    }

    @Test
    fun `search filters out foods without positive calories`() {
        val client = object : FoodDataClient {
            override fun searchFoodsGet(
                query: String,
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
                sortOrder: String?,
                brandOwner: String?,
            ): UsdaSearchResponse =
                UsdaSearchResponse(
                    totalHits = 3,
                    currentPage = 1,
                    totalPages = 1,
                    foods = listOf(
                        UsdaFoodItemResponse(
                            fdcId = 10,
                            description = "Zero kcal",
                            foodNutrients = listOf(
                                UsdaFoodNutrient(number = "1008", amount = 0.0),
                                UsdaFoodNutrient(number = "1003", amount = 1.0),
                                UsdaFoodNutrient(number = "1004", amount = 2.0),
                                UsdaFoodNutrient(number = "1005", amount = 3.0),
                            ),
                        ),
                        UsdaFoodItemResponse(
                            fdcId = 15,
                            description = "Macros but no energy row",
                            foodNutrients = listOf(
                                UsdaFoodNutrient(number = "1003", amount = 1.0),
                                UsdaFoodNutrient(number = "1004", amount = 2.0),
                                UsdaFoodNutrient(number = "1005", amount = 3.0),
                            ),
                        ),
                        UsdaFoodItemResponse(
                            fdcId = 20,
                            description = "Has kcal",
                            foodNutrients = listOf(
                                UsdaFoodNutrient(number = "1008", amount = 50.0),
                                UsdaFoodNutrient(number = "1003", amount = 1.0),
                                UsdaFoodNutrient(number = "1004", amount = 2.0),
                                UsdaFoodNutrient(number = "1005", amount = 3.0),
                            ),
                        ),
                    ),
                )

            override fun searchFoodsPost(
                query: String,
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
                sortOrder: String?,
                brandOwner: String?,
            ): UsdaSearchResponse = throw UnsupportedOperationException()

            override fun listFoodsGet(
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()

            override fun listFoodsPost(
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()

            override fun getFoodById(
                fdcId: Long,
                format: String?,
                nutrients: List<Long>,
            ): UsdaFoodItemResponse = throw UnsupportedOperationException()

            override fun getFoodsByIdsGet(
                fdcIds: List<Long>,
                format: String?,
                nutrients: List<Long>,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()

            override fun getFoodsByIdsPost(
                fdcIds: List<Long>,
                format: String?,
                nutrients: List<Long>,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()
        }

        val service = FoodService(client)
        val result = service.searchGet("x", 1, 25, emptyList(), null, null, null)

        assertEquals(1, result.foods.size)
        assertEquals(20L, result.foods.first().fdcId)
    }

    @Test
    fun `getById forwards format and nutrient filters to client`() {
        val client = object : FoodDataClient {
            override fun searchFoodsGet(
                query: String,
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
                sortOrder: String?,
                brandOwner: String?,
            ): UsdaSearchResponse = throw UnsupportedOperationException()

            override fun searchFoodsPost(
                query: String,
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
                sortOrder: String?,
                brandOwner: String?,
            ): UsdaSearchResponse = throw UnsupportedOperationException()

            override fun listFoodsGet(
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()

            override fun listFoodsPost(
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()

            override fun getFoodById(
                fdcId: Long,
                format: String?,
                nutrients: List<Long>,
            ): UsdaFoodItemResponse {
                assertEquals(12345L, fdcId)
                assertEquals("abridged", format)
                assertEquals(listOf(203L, 204L), nutrients)
                return UsdaFoodItemResponse(
                    fdcId = 12345L,
                    description = "Filtered Food",
                    foodNutrients = listOf(
                        UsdaFoodNutrient(number = "1003", amount = 1.0),
                        UsdaFoodNutrient(number = "1004", amount = 2.0),
                        UsdaFoodNutrient(number = "1005", amount = 3.0),
                    ),
                )
            }

            override fun getFoodsByIdsGet(
                fdcIds: List<Long>,
                format: String?,
                nutrients: List<Long>,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()

            override fun getFoodsByIdsPost(
                fdcIds: List<Long>,
                format: String?,
                nutrients: List<Long>,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()
        }

        val service = FoodService(client)
        val result = service.getById(12345L, "ABRIDGED", listOf(203L, 204L, 203L))

        assertEquals(12345L, result.fdcId)
        assertEquals("Filtered Food", result.description)
    }

    @Test
    fun `getById throws not found when macros are missing`() {
        val client = object : FoodDataClient {
            override fun searchFoodsGet(
                query: String,
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
                sortOrder: String?,
                brandOwner: String?,
            ): UsdaSearchResponse = throw UnsupportedOperationException()

            override fun searchFoodsPost(
                query: String,
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
                sortOrder: String?,
                brandOwner: String?,
            ): UsdaSearchResponse = throw UnsupportedOperationException()

            override fun listFoodsGet(
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()

            override fun listFoodsPost(
                pageNumber: Int,
                pageSize: Int,
                dataTypes: List<String>,
                sortBy: String?,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()

            override fun getFoodById(
                fdcId: Long,
                format: String?,
                nutrients: List<Long>,
            ): UsdaFoodItemResponse =
                UsdaFoodItemResponse(
                    fdcId = fdcId,
                    description = "Incomplete Food",
                    foodNutrients = listOf(
                        UsdaFoodNutrient(number = "1003", amount = 1.0),
                    ),
                )

            override fun getFoodsByIdsGet(
                fdcIds: List<Long>,
                format: String?,
                nutrients: List<Long>,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()

            override fun getFoodsByIdsPost(
                fdcIds: List<Long>,
                format: String?,
                nutrients: List<Long>,
            ): List<UsdaFoodItemResponse> = throw UnsupportedOperationException()
        }

        val service = FoodService(client)
        assertThrows(FoodNotFoundException::class.java) {
            service.getById(12345L, null, emptyList())
        }
    }
}
