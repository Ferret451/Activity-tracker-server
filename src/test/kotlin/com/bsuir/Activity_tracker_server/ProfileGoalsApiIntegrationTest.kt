package com.bsuir.Activity_tracker_server

import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ProfileGoalsApiIntegrationTest : ApiFunctionalCoverageIntegrationTest() {

    @Test
    fun `profile and goals endpoints work end-to-end`() {
        val accessToken = register("profile-goal-user").accessToken

        mockMvc.perform(
            put("/api/me/profile")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "weightKg": 78.5,
                      "heightCm": 180,
                      "age": 30,
                      "sex": "MALE",
                      "activityLevel": "MODERATE"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.weightKg").value(78.5))

        mockMvc.perform(get("/api/me/profile").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.heightCm").value(180))

        mockMvc.perform(get("/api/me/bmr").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.bmr").isNumber)
            .andExpect(jsonPath("$.dailyCalories").isNumber)

        val goalId = extractId(
            mockMvc.perform(
                post("/api/me/goals")
                    .withBearer(accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "goalType": "WEIGHT_KG",
                          "title": "Lose 3 kg",
                          "initialValue": 78.5,
                          "targetValue": 75.5,
                          "startDate": "2026-04-01",
                          "targetDate": "2026-06-01"
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

        mockMvc.perform(get("/api/me/goals").withBearer(accessToken))
            .andExpect(status().isOk)

        mockMvc.perform(get("/api/me/goals/$goalId").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(goalId))

        mockMvc.perform(
            post("/api/me/goals/$goalId/progress")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"value":76.0,"note":"First checkpoint"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentValue").value(76.0))

        mockMvc.perform(get("/api/me/goals/$goalId/progress").withBearer(accessToken))
            .andExpect(status().isOk)

        mockMvc.perform(
            put("/api/me/goals/$goalId/status")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"ARCHIVED"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ARCHIVED"))
    }
}
