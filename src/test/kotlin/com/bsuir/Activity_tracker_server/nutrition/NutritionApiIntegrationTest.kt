package com.bsuir.Activity_tracker_server.nutrition

import com.bsuir.Activity_tracker_server.food.client.UsdaFoodItemResponse
import com.bsuir.Activity_tracker_server.food.client.UsdaFoodNutrient
import com.bsuir.Activity_tracker_server.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class NutritionApiIntegrationTest : ApiIntegrationTest() {

    @Test
    fun `nutrition meal types food items records history and summary work`() {
        val accessToken = registerUser("nutrition").accessToken

        // meal types are seeded by Flyway (trigger after user insert), so pick the first existing one
        val mealTypeId = mockMvc.perform(get("/api/me/nutrition/meal-types").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").isNotEmpty)
            .andReturn()
            .response
            .contentAsString
            .let { objectMapper.readTree(it).path(0).path("id").asText() }

        // food item
        val foodItemId = mockMvc.perform(
            post("/api/me/nutrition/food-items")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Oats",
                      "caloriesPer100g": 380.0,
                      "proteinPer100g": 13.0,
                      "fatPer100g": 7.0,
                      "carbsPer100g": 67.0,
                      "source": "manual"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty)
            .andReturn()
            .response
            .contentAsString
            .let { objectMapper.readTree(it).path("id").asText() }

        // record
        val recordId = mockMvc.perform(
            post("/api/me/nutrition/records")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"mealTypeId":"$mealTypeId","foodItemId":"$foodItemId","grams":50.0}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty)
            .andExpect(jsonPath("$.totalCaloriesKcal").isNumber)
            .andReturn()
            .response
            .contentAsString
            .let { objectMapper.readTree(it).path("id").asText() }

        mockMvc.perform(get("/api/me/nutrition/records").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(recordId))

        mockMvc.perform(get("/api/me/nutrition/history").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].date").exists())

        mockMvc.perform(get("/api/me/nutrition/summary").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalCaloriesKcal").isNumber)
            .andExpect(jsonPath("$.byMealType[0].mealTypeId").value(mealTypeId))

        // USDA import path should use mocked FoodDataClient (no real HTTP)
        `when`(foodDataClient.getFoodById(12345L)).thenReturn(
            UsdaFoodItemResponse(
                fdcId = 12345L,
                description = "Apple",
                foodNutrients = listOf(
                    UsdaFoodNutrient(number = "1008", amount = 52.0),
                    UsdaFoodNutrient(number = "1003", amount = 0.3),
                    UsdaFoodNutrient(number = "1004", amount = 0.2),
                    UsdaFoodNutrient(number = "1005", amount = 13.8),
                ),
            ),
        )

        mockMvc.perform(post("/api/me/nutrition/food-items/import/usda/12345").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fdcId").value(12345))
            .andExpect(jsonPath("$.name").value("Apple"))
    }
}

