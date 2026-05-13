package com.bsuir.Activity_tracker_server.nutrition

import com.bsuir.Activity_tracker_server.food.client.UsdaFoodItemResponse
import com.bsuir.Activity_tracker_server.food.client.UsdaFoodNutrient
import com.bsuir.Activity_tracker_server.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class NutritionApiIntegrationTest : ApiIntegrationTest() {

    @Test
    fun `nutrition meal types food items records history and summary work`() {
        val accessToken = registerUser("nutrition").accessToken

        // meal types are global (Flyway seeds); pick any existing id from GET /api/me/nutrition/meal-types
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

        // USDA: один запрос «съел» — каталог + запись (раньше был отдельный import/usda)
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

        mockMvc.perform(
            post("/api/me/nutrition/records/intake")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "mealTypeId": "$mealTypeId",
                      "fdcId": 12345,
                      "grams": 100.0
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.foodItem.fdcId").value(12345))
            .andExpect(jsonPath("$.foodItem.name").value("Apple"))
            .andExpect(jsonPath("$.grams").value(100.0))
            .andExpect(jsonPath("$.totalCaloriesKcal").value(52.0))

        mockMvc.perform(
            post("/api/me/nutrition/records/intake")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "mealTypeId": "$mealTypeId",
                      "fdcId": 12345,
                      "grams": 50.0
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.grams").value(50.0))
            .andExpect(jsonPath("$.totalCaloriesKcal").value(26.0))
    }

    @Test
    fun `nutrition daily requires profile aggregates local zone day and rejects bad zone`() {
        val accessToken = registerUser("nutrition-daily").accessToken

        mockMvc.perform(get("/api/me/nutrition/daily").withBearer(accessToken))
            .andExpect(status().isNotFound)

        mockMvc.perform(
            put("/api/me/profile")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "weightKg": 70.0,
                      "heightCm": 180,
                      "age": 25,
                      "sex": "MALE",
                      "activityLevel": "MODERATE",
                      "calorieGoal": "MAINTAIN",
                      "dailyCalorieTargetKcal": 2000
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)

        val mealTypeId = mockMvc.perform(get("/api/me/nutrition/meal-types").withBearer(accessToken))
            .andExpect(status().isOk)
            .andReturn()
            .response
            .contentAsString
            .let { objectMapper.readTree(it).path(0).path("id").asText() }

        val foodItemId = mockMvc.perform(
            post("/api/me/nutrition/food-items")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "name": "Oats daily",
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
            .andReturn()
            .response
            .contentAsString
            .let { objectMapper.readTree(it).path("id").asText() }

        mockMvc.perform(
            post("/api/me/nutrition/records")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "mealTypeId": "$mealTypeId",
                      "foodItemId": "$foodItemId",
                      "grams": 50.0,
                      "consumedAt": "2026-06-01T10:00:00+03:00"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isCreated)

        mockMvc.perform(
            get("/api/me/nutrition/daily")
                .param("date", "2026-06-01")
                .param("zone", "Europe/Minsk")
                .withBearer(accessToken),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.date").value("2026-06-01"))
            .andExpect(jsonPath("$.zone").value("Europe/Minsk"))
            .andExpect(jsonPath("$.totalCaloriesKcal").value(190.0))
            .andExpect(jsonPath("$.totalProteinG").value(6.5))
            .andExpect(jsonPath("$.totalFatG").value(3.5))
            .andExpect(jsonPath("$.totalCarbsG").value(33.5))
            .andExpect(jsonPath("$.targetCaloriesKcal").value(2000.0))
            .andExpect(jsonPath("$.calorieTargetSource").value("EXPLICIT_DAILY_TARGET"))
            .andExpect(jsonPath("$.remainingCaloriesKcal").value(1810.0))

        mockMvc.perform(
            get("/api/me/nutrition/daily")
                .param("date", "2026-06-01")
                .param("zone", "NotARealZoneId")
                .withBearer(accessToken),
        )
            .andExpect(status().isBadRequest)
    }
}

