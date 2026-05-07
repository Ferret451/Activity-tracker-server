package com.bsuir.Activity_tracker_server.activity

import com.bsuir.Activity_tracker_server.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class ActivityApiIntegrationTest : ApiIntegrationTest() {

    @Test
    fun `activity types and records flow works with summary`() {
        val accessToken = registerUser("activity").accessToken

        // Creating activity records depends on having a profile (weight/height/etc.) for calorie calculations.
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
                      "activityLevel": "MODERATE"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isOk)

        val typeId = mockMvc.perform(
            post("/api/me/activity/types")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Running","met":8.0}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty)
            .andReturn()
            .response
            .contentAsString
            .let { objectMapper.readTree(it).path("id").asText() }

        mockMvc.perform(get("/api/me/activity/types").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(typeId))

        val recordId = mockMvc.perform(
            post("/api/me/activity/records")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"activityTypeId":"$typeId","durationMinutes":30,"distanceKm":5.0,"note":"test"}"""),
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").isNotEmpty)
            .andExpect(jsonPath("$.burnedCaloriesKcal").isNumber)
            .andReturn()
            .response
            .contentAsString
            .let { objectMapper.readTree(it).path("id").asText() }

        mockMvc.perform(get("/api/me/activity/records").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(recordId))

        mockMvc.perform(get("/api/me/activity/summary").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalDurationMinutes").value(30))
            .andExpect(jsonPath("$.byType[0].activityTypeId").value(typeId))

        mockMvc.perform(
            put("/api/me/activity/types/$typeId")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Jogging","met":7.0}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Jogging"))

        mockMvc.perform(delete("/api/me/activity/records/$recordId").withBearer(accessToken))
            .andExpect(status().isNoContent)

        mockMvc.perform(delete("/api/me/activity/types/$typeId").withBearer(accessToken))
            .andExpect(status().isNoContent)
    }
}

