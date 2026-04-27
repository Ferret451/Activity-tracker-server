package com.bsuir.Activity_tracker_server

import com.bsuir.Activity_tracker_server.food.client.UsdaSearchResponse
import com.bsuir.Activity_tracker_server.food.exception.ExternalFoodApiException
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class FoodApiIntegrationTest : ApiFunctionalCoverageIntegrationTest() {

    @Test
    fun `food endpoints support full USDA wrapper contract and map upstream errors`() {
        val accessToken = register("food-user").accessToken

        val usdaFood = usdaFood(
            fdcId = 12345L,
            description = "Apple",
            calories = 52.0,
            protein = 0.3,
            fat = 0.2,
            carbs = 13.8,
        )

        `when`(foodDataClient.searchFoodsGet(anyString(), anyInt(), anyInt(), anyList(), anyString(), anyString(), anyString()))
            .thenReturn(UsdaSearchResponse(totalHits = 1, currentPage = 1, totalPages = 1, foods = listOf(usdaFood)))
        `when`(foodDataClient.searchFoodsPost(anyString(), anyInt(), anyInt(), anyList(), anyString(), anyString(), anyString()))
            .thenReturn(UsdaSearchResponse(totalHits = 1, currentPage = 1, totalPages = 1, foods = listOf(usdaFood)))
        `when`(foodDataClient.listFoodsGet(anyInt(), anyInt(), anyList(), anyString())).thenReturn(listOf(usdaFood))
        `when`(foodDataClient.listFoodsPost(anyInt(), anyInt(), anyList(), anyString())).thenReturn(listOf(usdaFood))
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
            post("/api/foods/list")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"pageNumber":1,"pageSize":10}"""),
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

        mockMvc.perform(
            post("/api/foods")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"fdcIds":[12345],"format":"abridged"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].fdcId").value(12345))

        `when`(foodDataClient.searchFoodsGet(anyString(), anyInt(), anyInt(), anyList(), anyString(), anyString(), anyString()))
            .thenThrow(ExternalFoodApiException(HttpStatus.TOO_MANY_REQUESTS, "limit"))

        mockMvc.perform(
            get("/api/foods/search")
                .withBearer(accessToken)
                .param("query", "apple")
                .param("pageNumber", "1")
                .param("pageSize", "10"),
        )
            .andExpect(status().isTooManyRequests)
            .andExpect(jsonPath("$.detail").exists())
    }
}
