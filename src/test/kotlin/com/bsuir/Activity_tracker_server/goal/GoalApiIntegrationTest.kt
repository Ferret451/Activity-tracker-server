package com.bsuir.Activity_tracker_server.goal

import com.bsuir.Activity_tracker_server.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

class GoalApiIntegrationTest : ApiIntegrationTest() {

    @Test
    fun `goals create get and progress work`() {
        val accessToken = registerUser("goal").accessToken

        val goalId = mockMvc.perform(
            post("/api/me/goals")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "goalType": "WEIGHT_KG",
                      "title": "Lose weight",
                      "initialValue": 80.0,
                      "targetValue": 75.0,
                      "startDate": "${LocalDate.now()}",
                      "targetDate": "${LocalDate.now().plusDays(7)}"
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

        mockMvc.perform(get("/api/me/goals").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").value(goalId))

        mockMvc.perform(get("/api/me/goals/$goalId").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(goalId))

        mockMvc.perform(
            post("/api/me/goals/$goalId/progress")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"value": 79.0, "note": "day1"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentValue").isNumber)

        mockMvc.perform(get("/api/me/goals/$goalId/progress").withBearer(accessToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].id").isNotEmpty)

        mockMvc.perform(
            put("/api/me/goals/$goalId/status")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"ARCHIVED"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("ARCHIVED"))
    }

    @Test
    fun `goals validation returns 400`() {
        val accessToken = registerUser("goal-val").accessToken

        mockMvc.perform(
            post("/api/me/goals")
                .withBearer(accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "goalType": "WEIGHT_KG",
                      "title": "",
                      "initialValue": -1,
                      "targetValue": -1,
                      "startDate": "${LocalDate.now()}",
                      "targetDate": "${LocalDate.now().minusDays(1)}"
                    }
                    """.trimIndent(),
                ),
        )
            .andExpect(status().isBadRequest)
    }
}

