package com.bsuir.Activity_tracker_server

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class NutritionApiIntegrationTest : ApiFunctionalCoverageIntegrationTest() {

    @Test
    fun `nutrition endpoints work end-to-end including USDA import`() {
        val accessToken = register("nutrition-user").accessToken

        val mealTypeId = extractId(
            mockMvc.perform(
                post("/api/me/nutrition/meal-types")
                    .withBearer(accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Brunch"}"""),
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").isNotEmpty)
                .andReturn()
                .response
                .contentAsString,
        )

        mockMvc.perform(get("/api/me/nutrition/meal-types").withBearer(accessToken))
            .andExpect(status().isOk)

        mockMvc.perform(
            put("/api/me/nutrition/meal-types/$mealTypeId")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Brunch Updated"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Brunch Updated"))

        val foodItemId = extractId(
            mockMvc.perform(
                post("/api/me/nutrition/food-items")
                    .withBearer(accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "Oatmeal",
                          "caloriesPer100g": 389.0,
                          "proteinPer100g": 16.9,
                          "fatPer100g": 6.9,
                          "carbsPer100g": 66.3,
                          "source": "manual",
                          "note": "Breakfast staple"
                        }
                        """.trimIndent(),
                    ),
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").isNotEmpty)
                .andReturn()
                .response
                .contentAsString,
        )

        mockMvc.perform(get("/api/me/nutrition/food-items").withBearer(accessToken))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/me/nutrition/food-items/$foodItemId").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(foodItemId))

        mockMvc.perform(
            put("/api/me/nutrition/food-items/$foodItemId")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Oatmeal Updated",
                      "caloriesPer100g": 380.0,
                      "proteinPer100g": 15.0,
                      "fatPer100g": 7.0,
                      "carbsPer100g": 65.0,
                      "source": "manual",
                      "note": "Updated"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Oatmeal Updated"))

        `when`(foodDataClient.getFoodById(anyLong(), anyString(), anyList())).thenReturn(
            usdaFood(
                fdcId = 99999L,
                description = "USDA Banana",
                calories = 89.0,
                protein = 1.1,
                fat = 0.3,
                carbs = 22.8,
            ),
        )

        mockMvc.perform(post("/api/me/nutrition/food-items/import/usda/99999").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.fdcId").value(99999))
            .andExpect(jsonPath("$.source").value("USDA"))

        val recordId = extractId(
            mockMvc.perform(
                post("/api/me/nutrition/records")
                    .withBearer(accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "mealTypeId": "$mealTypeId",
                          "foodItemId": "$foodItemId",
                          "grams": 80.0,
                          "note": "Morning portion",
                          "consumedAt": "2026-04-22T08:00:00Z"
                        }
                        """.trimIndent(),
                    ),
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.id").isNotEmpty)
                .andReturn()
                .response
                .contentAsString,
        )

        mockMvc.perform(get("/api/me/nutrition/records").withBearer(accessToken))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/me/nutrition/records/$recordId").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(recordId))

        mockMvc.perform(
            put("/api/me/nutrition/records/$recordId")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "mealTypeId": "$mealTypeId",
                      "foodItemId": "$foodItemId",
                      "grams": 90.0,
                      "note": "Updated portion",
                      "consumedAt": "2026-04-22T08:00:00Z"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.grams").value(90.0))

        mockMvc.perform(get("/api/me/nutrition/history").withBearer(accessToken))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/me/nutrition/summary").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalCaloriesKcal").isNumber)

        mockMvc.perform(delete("/api/me/nutrition/records/$recordId").withBearer(accessToken))
            .andExpect(status().isNoContent)

        mockMvc.perform(delete("/api/me/nutrition/food-items/$foodItemId").withBearer(accessToken))
            .andExpect(status().isNoContent)

        mockMvc.perform(delete("/api/me/nutrition/meal-types/$mealTypeId").withBearer(accessToken))
            .andExpect(status().isNoContent)
    }
}
