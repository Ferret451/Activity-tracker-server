package com.bsuir.Activity_tracker_server.food

import com.bsuir.Activity_tracker_server.food.client.UsdaFoodItemResponse
import com.bsuir.Activity_tracker_server.food.client.UsdaFoodNutrient
import com.bsuir.Activity_tracker_server.food.client.UsdaSearchResponse
import com.bsuir.Activity_tracker_server.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.isNull
import org.mockito.Mockito.`when`
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class FoodApiIntegrationTest : ApiIntegrationTest() {

    @Test
    fun `food endpoints work with mocked USDA client`() {
        val accessToken = registerUser("food").accessToken

        val usdaFood = UsdaFoodItemResponse(
            fdcId = 12345L,
            description = "Apple",
            foodNutrients = listOf(
                UsdaFoodNutrient(number = "1008", amount = 52.0),
                UsdaFoodNutrient(number = "1003", amount = 0.3),
                UsdaFoodNutrient(number = "1004", amount = 0.2),
                UsdaFoodNutrient(number = "1005", amount = 13.8),
            ),
        )

        `when`(
            foodDataClient.searchFoodsGet(
                anyString(),
                anyInt(),
                anyInt(),
                anyList(),
                isNull(),
                isNull(),
                isNull(),
            ),
        )
            .thenReturn(UsdaSearchResponse(totalHits = 1, currentPage = 1, totalPages = 1, foods = listOf(usdaFood)))
        `when`(
            foodDataClient.searchFoodsPost(
                anyString(),
                anyInt(),
                anyInt(),
                anyList(),
                isNull(),
                isNull(),
                isNull(),
            ),
        )
            .thenReturn(UsdaSearchResponse(totalHits = 1, currentPage = 1, totalPages = 1, foods = listOf(usdaFood)))
        `when`(foodDataClient.listFoodsGet(anyInt(), anyInt(), anyList(), isNull())).thenReturn(listOf(usdaFood))
        `when`(foodDataClient.listFoodsPost(anyInt(), anyInt(), anyList(), isNull())).thenReturn(listOf(usdaFood))
        `when`(foodDataClient.getFoodById(anyLong(), anyString(), anyList())).thenReturn(usdaFood)
        `when`(foodDataClient.getFoodsByIdsGet(anyList(), anyString(), anyList())).thenReturn(listOf(usdaFood))
        `when`(foodDataClient.getFoodsByIdsPost(anyList(), anyString(), anyList())).thenReturn(listOf(usdaFood))

        mockMvc.perform(
            get("/api/foods/search")
                .withBearer(accessToken)
                .param("query", "apple")
                .param("pageNumber", "1")
                .param("pageSize", "10"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.foods[0].fdcId").value(12345))

        mockMvc.perform(
            post("/api/foods/search")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"query":"apple","pageSize":10,"pageNumber":1}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.foods[0].description").value("Apple"))

        mockMvc.perform(
            get("/api/foods/list")
                .withBearer(accessToken)
                .param("pageNumber", "1")
                .param("pageSize", "10"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].fdcId").value(12345))

        mockMvc.perform(
            get("/api/foods/12345")
                .withBearer(accessToken)
                .param("format", "abridged")
                .param("nutrients", "1003")
                .param("nutrients", "1004"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fdcId").value(12345))
            .andExpect(jsonPath("$.nutrients.caloriesKcal").value(52.0))

        mockMvc.perform(
            get("/api/foods")
                .withBearer(accessToken)
                .param("fdcIds", "12345", "54321")
                .param("format", "full"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].fdcId").value(12345))

        mockMvc.perform(
            post("/api/foods/lookup")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"fdcIds":[12345,54321],"format":"full"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].fdcId").value(12345))
    }

    @Test
    fun `food validation returns 400`() {
        val accessToken = registerUser("food-val").accessToken

        mockMvc.perform(get("/api/foods/search").withBearer(accessToken).param("query", ""))
            .andExpect(status().isBadRequest)
    }
}

