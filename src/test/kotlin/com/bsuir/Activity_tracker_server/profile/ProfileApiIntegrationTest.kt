package com.bsuir.Activity_tracker_server.profile

import com.bsuir.Activity_tracker_server.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ProfileApiIntegrationTest : ApiIntegrationTest() {

    @Test
    fun `profile upsert and bmr calculation work`() {
        val accessToken = registerUser("profile").accessToken

        mockMvc.perform(
            put("/api/me/profile")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "weightKg": 70.5,
                      "heightCm": 180,
                      "age": 25,
                      "sex": "MALE",
                      "activityLevel": "MODERATE",
                      "calorieGoal": "MAINTAIN"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.weightKg").value(70.5))
            .andExpect(jsonPath("$.heightCm").value(180))

        mockMvc.perform(get("/api/me/profile").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sex").value("MALE"))

        mockMvc.perform(get("/api/me/bmr").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.bmr").isNumber)
            .andExpect(jsonPath("$.baseDailyCalories").isNumber)
            .andExpect(jsonPath("$.goalAdjustedDailyCalories").isNumber)
    }

    @Test
    fun `profile validation returns 400`() {
        val accessToken = registerUser("profile-val").accessToken

        mockMvc.perform(
            put("/api/me/profile")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "weightKg": -1,
                      "heightCm": 10,
                      "age": 5,
                      "sex": "MALE",
                      "activityLevel": "LOW",
                      "calorieGoal": "MAINTAIN"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
    }
}

